package com.protoss.tansfertool.thread;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CountDirFilesTask extends Task<Map<String, Long>> {
    private static final Logger log = LoggerFactory.getLogger(CountDirFilesTask.class);

    // 可配置参数
    private final long timeoutMinutes; // 超时时间（分钟）
    private final int estimationSampleSize; // 用于估算的样本大小

    private final AtomicLong totalSize = new AtomicLong(0);
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong estimatedTotalFiles = new AtomicLong(0);
    private final List<File> files;
    private volatile boolean hasError = false;

    // 使用ForkJoinPool处理文件遍历任务
    private final ForkJoinPool fileProcessingPool;

    public CountDirFilesTask(List<File> files) {
        this(files, 1440, 10000); // 默认60分钟超时，10000个文件用于估算
    }

    public CountDirFilesTask(List<File> files, long timeoutMinutes, int estimationSampleSize) {
        this.files = files;
        this.timeoutMinutes = timeoutMinutes;
        this.estimationSampleSize = estimationSampleSize;

        // 使用可用处理器数量作为并行级别的基础
        int parallelism = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.fileProcessingPool = new ForkJoinPool(parallelism);
    }

    @Override
    protected Map<String, Long> call() throws Exception {
        try {
            // 第一阶段：快速估算总文件数
            estimateTotalFileCount();

            // 第二阶段：详细处理文件
            long size = processFiles();

            Map<String, Long> result = new HashMap<>();
            result.put("size", size);
            result.put("count", totalCount.get());
            return result;
        } finally {
            // 确保线程池正确关闭
            shutdownExecutorService();
        }
    }

    private void estimateTotalFileCount() {
        try {
            log.info("开始估算文件总数...");

            // 使用采样方法估算总文件数
            final AtomicLong sampleCount = new AtomicLong(0);
            final AtomicLong sampleTotal = new AtomicLong(0);

            for (File rootFile : files) {
                if (!rootFile.exists()) continue;

                // 采样一小部分文件夹来估算
                if (rootFile.isDirectory()) {
                    Path rootPath = rootFile.toPath();

                    Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                        private int dirCount = 0;
                        private int fileCount = 0;

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            dirCount++;
                            // 采样达到上限时停止
                            return (dirCount + fileCount < estimationSampleSize)
                                    ? FileVisitResult.CONTINUE
                                    : FileVisitResult.SKIP_SUBTREE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            fileCount++;
                            sampleCount.incrementAndGet();
                            // 采样达到上限时停止
                            return (dirCount + fileCount < estimationSampleSize)
                                    ? FileVisitResult.CONTINUE
                                    : FileVisitResult.TERMINATE;
                        }
                    });

                    // 根据采样估算总文件数
                    try {
                        long totalSpace = rootFile.getTotalSpace();
                        long usableSpace = rootFile.getUsableSpace();
                        long estimatedUsedSpace = totalSpace - usableSpace;

                        // 这只是一个粗略估计，实际应用中需要更精确的算法
                        long estimatedFiles = (sampleCount.get() > 0)
                                ? (long)(estimatedUsedSpace * 0.0001) // 粗略估算，假设每个文件平均10KB
                                : 0;

                        sampleTotal.addAndGet(estimatedFiles);
                    } catch (Exception e) {
                        log.warn("估算文件数量时出错", e);
                    }
                } else {
                    sampleTotal.incrementAndGet(); // 单个文件
                }
            }

            // 设置估算的总文件数（至少为1）
            long estimated = Math.max(1, sampleTotal.get());
            estimatedTotalFiles.set(estimated);
            log.info("估算的总文件数: {}", estimated);

        } catch (Exception e) {
            log.error("估算文件数量失败", e);
            // 设置一个默认值，避免分母为0
            estimatedTotalFiles.set(10000);
        }
    }

    private long processFiles() throws InterruptedException, ExecutionException {
        final CountDownLatch completionLatch = new CountDownLatch(1);

        try {
            // 提交所有根目录/文件处理任务到ForkJoinPool
            fileProcessingPool.submit(() -> {
                try {
                    // 并行处理所有输入文件
                    files.parallelStream()
                            .filter(File::exists)
                            .forEach(file -> processFileOrDirectoryParallel(file));
                } finally {
                    completionLatch.countDown();
                }
            });

            // 等待处理完成或超时
            if (!completionLatch.await(timeoutMinutes, TimeUnit.MINUTES)) {
                log.warn("文件统计超时（{}分钟）- 已处理: {}/{} 文件",
                        timeoutMinutes, processedCount.get(), estimatedTotalFiles.get());

                // 即使超时也返回已处理的结果
                return totalSize.get();
            }

            log.info("文件统计完成 - 总计: {} 文件, {} 字节", totalCount.get(), totalSize.get());
            return totalSize.get();

        } catch (Exception e) {
            log.error("处理文件时发生错误", e);
            hasError = true;
            throw e;
        }
    }

    private void processFileOrDirectoryParallel(File file) {
        if (isCancelled()) return;

        try {
            if (file.isFile()) {
                processFile(file);
            } else if (file.isDirectory()) {
                // 使用NIO处理目录，效率更高
                processDirectoryWithNIO(file.toPath());
            }
        } catch (Exception e) {
            log.error("处理文件/目录时出错: {}", file.getAbsolutePath(), e);
        }
    }

    private void processDirectoryWithNIO(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isCancelled()) return FileVisitResult.TERMINATE;

                    try {
                        long fileSize = attrs.size();
                        totalSize.addAndGet(fileSize);
                        totalCount.incrementAndGet();
                        long processed = processedCount.incrementAndGet();

                        // 更新进度 - 使用估算的总数作为参考
                        updateProgress(processed, estimatedTotalFiles.get());

                        // 定期记录进度
                        if (processed % 100000 == 0) {
                            log.info("已处理: {} 文件, 估计进度: {}/{} ({:.2f}%)",
                                    processed, processed, estimatedTotalFiles.get(),
                                    (100.0 * processed / estimatedTotalFiles.get()));
                        }
                    } catch (Exception e) {
                        log.warn("处理文件时出错: {}", file, e);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("无法访问文件: {}", file, exc);
                    return FileVisitResult.CONTINUE; // 继续处理其他文件
                }
            });
        } catch (IOException e) {
            log.error("遍历目录失败: {}", directory, e);
        }
    }

    private void processFile(File file) {
        try {
            long fileSize = file.length();
            totalSize.addAndGet(fileSize);
            totalCount.incrementAndGet();
            long processed = processedCount.incrementAndGet();

            // 更新进度 - 使用估算的总数作为参考
            updateProgress(processed, estimatedTotalFiles.get());
        } catch (Exception e) {
            log.error("处理文件时出错: {}", file.getAbsolutePath(), e);
        }
    }

    private void shutdownExecutorService() {
        try {
            fileProcessingPool.shutdown();
            if (!fileProcessingPool.awaitTermination(1, TimeUnit.MINUTES)) {
                fileProcessingPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            fileProcessingPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        shutdownExecutorService();
    }
}