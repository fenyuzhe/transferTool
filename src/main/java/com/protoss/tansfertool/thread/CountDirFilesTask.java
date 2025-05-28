package com.protoss.tansfertool.thread;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CountDirFilesTask 是一个 JavaFX 后台任务，用于并行统计指定目录/文件列表中的文件数量与总大小。
 * 它分为估算阶段和正式统计阶段，并使用 ForkJoinPool 进行多线程加速。
 */
public class CountDirFilesTask extends Task<Map<String, Long>> {
    private static final Logger log = LoggerFactory.getLogger(CountDirFilesTask.class);

    private static final int MAX_DEPTH = 64;

    private final long timeoutMinutes;
    private final int estimationSampleSize;

    private final AtomicLong totalSize = new AtomicLong(0);
    private final AtomicLong totalCount = new AtomicLong(0);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong estimatedTotalFiles = new AtomicLong(0);

    private final List<File> files;
    private final ForkJoinPool fileProcessingPool;

    public CountDirFilesTask(List<File> files) {
        this(files, 60, 10000);
    }

    public CountDirFilesTask(List<File> files, long timeoutMinutes, int estimationSampleSize) {
        this.files = files;
        this.timeoutMinutes = timeoutMinutes;
        this.estimationSampleSize = estimationSampleSize;

        int parallelism = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.fileProcessingPool = new ForkJoinPool(parallelism, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    }

    @Override
    protected Map<String, Long> call() throws Exception {
        try {
            estimateTotalFileCount();

            ForkJoinTask<Void> task = fileProcessingPool.submit(new DirectoryProcessingAction(files));
            task.get(timeoutMinutes, TimeUnit.MINUTES);

            log.info("文件统计完成 - 总计: {} 文件, {} 字节", totalCount.get(), totalSize.get());

            Map<String, Long> result = new HashMap<>();
            result.put("size", totalSize.get());
            result.put("count", totalCount.get());
            return result;
        } catch (TimeoutException e) {
            log.warn("任务超时，已处理: {} 文件", processedCount.get());
            return Map.of("size", totalSize.get(), "count", totalCount.get());
        } finally {
            shutdownExecutorService();
        }
    }

    private void estimateTotalFileCount() {
        try {
            log.info("开始估算文件总数...");
            AtomicLong sampleCount = new AtomicLong(0);
            AtomicLong totalFileSize = new AtomicLong(0);

            for (File rootFile : files) {
                if (!rootFile.exists()) continue;

                if (rootFile.isDirectory()) {
                    Files.walkFileTree(rootFile.toPath(), EnumSet.noneOf(FileVisitOption.class), MAX_DEPTH, new SimpleFileVisitor<>() {
                        int visited = 0;

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (sampleCount.incrementAndGet() >= estimationSampleSize) return FileVisitResult.TERMINATE;
                            totalFileSize.addAndGet(attrs.size());
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } else {
                    sampleCount.incrementAndGet();
                    totalFileSize.addAndGet(rootFile.length());
                }

                if (sampleCount.get() >= estimationSampleSize) break;
            }

            long avgSize = sampleCount.get() > 0 ? totalFileSize.get() / sampleCount.get() : 10240;
            long estimated = totalFileSize.get() > 0 ? (rootFileSpaceUsed() / Math.max(avgSize, 1024)) : 10000;
            estimatedTotalFiles.set(Math.max(estimated, 1));

            log.info("估算的总文件数: {}", estimatedTotalFiles.get());
        } catch (Exception e) {
            log.error("估算文件数量失败", e);
            estimatedTotalFiles.set(10000);
        }
    }

    private long rootFileSpaceUsed() {
        return files.stream()
                .mapToLong(f -> f.exists() ? f.getTotalSpace() - f.getUsableSpace() : 0)
                .sum();
    }

    private void processFile(Path file, BasicFileAttributes attrs) {
        try {
            totalSize.addAndGet(attrs.size());
            totalCount.incrementAndGet();
            long processed = processedCount.incrementAndGet();

            // 控制 UI 更新频率
            if (processed % 100 == 0) {
                updateProgress(processed, estimatedTotalFiles.get());
            }
        } catch (Exception e) {
            log.warn("处理文件失败: {}", file, e);
        }
    }

    private class DirectoryProcessingAction extends RecursiveAction {
        private final List<File> targets;

        public DirectoryProcessingAction(List<File> targets) {
            this.targets = targets;
        }

        @Override
        protected void compute() {
            List<RecursiveAction> actions = new ArrayList<>();

            for (File file : targets) {
                if (isCancelled()) return;
                if (!file.exists()) continue;

                if (file.isFile()) {
                    actions.add(new FileAction(file.toPath()));
                } else if (file.isDirectory()) {
                    actions.add(new DirectoryAction(file.toPath()));
                }
            }

            invokeAll(actions);
        }
    }

    private class DirectoryAction extends RecursiveAction {
        private final Path dir;

        public DirectoryAction(Path dir) {
            this.dir = dir;
        }

        @Override
        protected void compute() {
            try {
                Files.walkFileTree(dir, EnumSet.noneOf(FileVisitOption.class), MAX_DEPTH, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isCancelled()) return FileVisitResult.TERMINATE;
                        processFile(file, attrs);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        log.warn("无法访问文件: {}", file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.error("遍历目录失败: {}", dir, e);
            }
        }
    }

    private class FileAction extends RecursiveAction {
        private final Path file;

        public FileAction(Path file) {
            this.file = file;
        }

        @Override
        protected void compute() {
            try {
                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                processFile(file, attrs);
            } catch (IOException e) {
                log.error("读取文件属性失败: {}", file, e);
            }
        }
    }

    private void shutdownExecutorService() {
        fileProcessingPool.shutdown();
        try {
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
