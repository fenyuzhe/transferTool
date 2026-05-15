package com.protoss.toolkit.thread;

import com.protoss.toolkit.codec.TranscoderMain;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import org.dcm4che3.tool.dcm2dcm.Dcm2Dcm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static com.protoss.toolkit.util.TransferFileUtil.commitGeneratedFile;
import static com.protoss.toolkit.util.TransferFileUtil.copyFileSafely;
import static com.protoss.toolkit.util.TransferFileUtil.createTempPath;
import static com.protoss.toolkit.util.TransferFileUtil.deleteQuietly;

public class TransferTask extends Task<Integer> {
    private static Logger log = LoggerFactory.getLogger(TransferTask.class);
    /** 移动策略标识，替代散落在代码各处的魔法字符串 */
    private static final String STRATEGY_MOVE = "移动";
    final private AtomicLong totalCount = new AtomicLong();
    private final AtomicLong transferredBytes = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();
    private final AtomicLong skippedCount = new AtomicLong();
    private final AtomicLong lastUiUpdateTime = new AtomicLong();
    private final List<String> failedFiles = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean pause = false;
    private volatile long startTime;
    private volatile String currentPath = "";
    private List<File> sourceFileList;
    private String sourceFilePath;
    private String targetFile;
    private String strategy;
    private boolean isCompress;
    private long filterSize;
    private int threads;
    private String tanscode;
    private long count;
    private final List<TransferThread> threadList = Collections.synchronizedList(new ArrayList<>());
    private String compressMode;

    private final StringProperty transferredFilesText = new SimpleStringProperty("0");
    private final StringProperty transferredSizeText = new SimpleStringProperty("0 B");
    private final StringProperty speedText = new SimpleStringProperty("0 B/s");
    private final StringProperty elapsedText = new SimpleStringProperty("00:00:00");
    private final StringProperty currentPathText = new SimpleStringProperty("-");
    private final StringProperty errorSkipText = new SimpleStringProperty("0 / 0");

    private String pattern = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})(((0[13578]|1[02])(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)(0[1-9]|[12][0-9]|30))|(02(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))0229)";

    public TransferTask(String compressMode, long count, List<File> sourceFileList, String sourceFilePath,
            String targetFile, String strategy, boolean isCompress, String tanscode, long filterSize, int threads) {
        this.compressMode = compressMode;
        this.sourceFileList = sourceFileList;
        this.sourceFilePath = sourceFilePath;
        this.targetFile = targetFile;
        this.strategy = strategy;
        this.isCompress = isCompress;
        this.filterSize = filterSize;
        this.threads = threads;
        this.tanscode = tanscode;
        this.count = count;
    }

    @Override
    protected Integer call() throws Exception {
        validateTransferRoots();
        startTime = System.currentTimeMillis();
        publishRuntimeMetrics(true);
        List<File> list = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);
        for (File f : sourceFileList) {
            checkFile(f, list, p);
        }
        log.info("待转移文件夹: {} ===list大小: {}", list, list.size());
        if (list.size() > threads) {
            int perThreadCount = list.size() / threads; // 每个线程处理的文件夹数
            int remainCount = list.size() % threads; // 处理不完的文件夹数
            int startIndex = 0; // 每个线程开始处理的文件夹数下标
            for (int i = 0; i < threads; i++) {
                int endIndex = (i < remainCount) ? startIndex + perThreadCount + 1 : startIndex + perThreadCount;
                List<File> subList = list.subList(startIndex, endIndex);
                log.info("{}===subList: {} ===subList大小: {}", i, subList, subList.size());
                TransferThread thread = new TransferThread(subList, sourceFilePath, targetFile, strategy, isCompress,
                        tanscode, filterSize, compressMode);
                threadList.add(thread);
                startIndex = endIndex;
                thread.start();
            }
        } else {
            for (File file : list) {
                List<File> subList = Collections.singletonList(file);
                TransferThread thread = new TransferThread(subList, sourceFilePath, targetFile, strategy, isCompress,
                        tanscode, filterSize, compressMode);
                log.info("{}-单独线程处理: {}", thread.getName(), subList);
                threadList.add(thread);
                thread.start();
            }
        }
        List<TransferThread> threadsSnapshot;
        synchronized (threadList) {
            threadsSnapshot = new ArrayList<>(threadList);
        }
        for (TransferThread thread : threadsSnapshot) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.error("Thread interrupted", e);
                Thread.currentThread().interrupt(); // 恢复中断标志，允许任务被正确取消
                throw e;
            }
        }
        publishRuntimeMetrics(true);
        log.info("Transfer elapsed: {} ms", System.currentTimeMillis() - startTime);

        if (failedCount.get() > 0) {
            throw new IOException("Transfer failed for " + failedCount.get() + " file(s): " + failedFiles);
        }

        // 如果是移动策略，且任务顺利完成，进行空文件夹清理
        if (STRATEGY_MOVE.equals(strategy)) {
            log.info("开始清理空文件夹...");
            deleteEmptyDirs(new File(sourceFilePath));
        }

        return 1;
    }

    public StringProperty transferredFilesTextProperty() {
        return transferredFilesText;
    }

    public StringProperty transferredSizeTextProperty() {
        return transferredSizeText;
    }

    public StringProperty speedTextProperty() {
        return speedText;
    }

    public StringProperty elapsedTextProperty() {
        return elapsedText;
    }

    public StringProperty currentPathTextProperty() {
        return currentPathText;
    }

    public StringProperty errorSkipTextProperty() {
        return errorSkipText;
    }

    private void validateTransferRoots() throws IOException {
        Path sourceRoot = toComparablePath(Paths.get(sourceFilePath));
        Path targetRoot = toComparablePath(Paths.get(targetFile));
        if (sourceRoot.equals(targetRoot)) {
            throw new IOException("Source and target directories must be different: " + sourceRoot);
        }
        if (targetRoot.startsWith(sourceRoot)) {
            throw new IOException("Target directory must not be inside source directory: " + targetRoot);
        }
    }

    private Path toComparablePath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        try {
            return normalized.toRealPath();
        } catch (IOException e) {
            Path parent = normalized.getParent();
            if (parent != null && Files.exists(parent)) {
                try {
                    return parent.toRealPath().resolve(normalized.getFileName()).normalize();
                } catch (IOException ignored) {
                    return normalized;
                }
            }
            return normalized;
        }
    }

    private void deleteEmptyDirs(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteEmptyDirs(f);
                }
            }
        }
        files = dir.listFiles();
        if (files == null || files.length == 0) {
            // 不删除根目录
            if (!dir.getAbsolutePath().equals(sourceFilePath)) {
                if (dir.delete()) {
                    log.info("Deleted empty directory: {}", dir.getAbsolutePath());
                } else {
                    log.warn("Failed to delete empty directory: {}", dir.getAbsolutePath());
                }
            }
        }
    }

    public void pause() {
        pause = true;
        List<TransferThread> threadsSnapshot;
        synchronized (threadList) {
            threadsSnapshot = new ArrayList<>(threadList);
        }
        for (TransferThread thread : threadsSnapshot) {
            thread.pause();
        }
    }

    public void resume() {
        pause = false;
        List<TransferThread> threadsSnapshot;
        synchronized (threadList) {
            threadsSnapshot = new ArrayList<>(threadList);
        }
        for (TransferThread thread : threadsSnapshot) {
            thread.resumeTransfer();
        }
    }

    class TransferThread extends Thread {
        private volatile boolean pause = false;
        private final List<File> sourceFileList;
        private final String strategy;
        private final boolean isCompress;
        private final long filterSize;
        private final String tanscode;
        private final String compressMode;
        /** 缓存解析好的路径，避免每次处理文件时重复计算 */
        private final Path sourceRoot;
        private final Path targetRoot;

        public TransferThread(List<File> sourceFileList, String sourceFilePath, String targetFile, String strategy,
                boolean isCompress, String tanscode, long filterSize, String compressMode) {
            this.sourceFileList = sourceFileList;
            this.strategy = strategy;
            this.isCompress = isCompress;
            this.filterSize = filterSize;
            this.tanscode = tanscode;
            this.compressMode = compressMode;
            // 在构造时计算并缓存，避免每个文件都重复解析
            this.sourceRoot = Paths.get(sourceFilePath).toAbsolutePath().normalize();
            this.targetRoot = Paths.get(targetFile).toAbsolutePath().normalize();
        }

        public void run() {
            for (File sourceFile : sourceFileList) {
                transfer(sourceFile);
            }
        }

        public void transfer(File f) {
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                for (final File f1 : files) {
                    try {
                        waitIfPaused();
                    } catch (InterruptedException e) {
                        log.error("线程中断", e);
                        Thread.currentThread().interrupt();
                        return;
                    }

                    if (f1.isFile()) {
                        try {
                            currentPath = f1.getAbsolutePath();
                            publishRuntimeMetrics(false);
                            long sourceSize = Math.max(0, f1.length());
                            File desFile = resolveTargetFile(f1);
                            transferOneFile(f1, desFile);

                            if (desFile.exists()) {
                                boolean transferComplete = true;
                                if (STRATEGY_MOVE.equals(strategy)) {
                                    deleteSourceFile(f1);
                                }
                                if (transferComplete) {
                                    totalCount.incrementAndGet();
                                    transferredBytes.addAndGet(sourceSize);
                                    publishRuntimeMetrics(true);
                                }
                            }
                        } catch (Exception e) {
                            recordFailure(f1, e);
                            publishRuntimeMetrics(true);
                            log.error("Error transferring file: {}", f1.getAbsolutePath(), e);
                        }
                    } else {
                        transfer(f1);
                    }
                }
            } else if (f.isDirectory()) {
                skippedCount.incrementAndGet();
                currentPath = f.getAbsolutePath();
                publishRuntimeMetrics(true);
            }

            // 目录的清理工作由 call() 末尾的 deleteEmptyDirs() 统一负责，
            // 此处不做提前删除，避免多线程场景下的竞态条件。
        }

        private File resolveTargetFile(File sourceFile) throws IOException {
            // 使用构造时缓存的 sourceRoot/targetRoot，无需重复解析
            Path sourcePath = sourceFile.toPath().toAbsolutePath().normalize();

            if (!sourcePath.startsWith(sourceRoot)) {
                throw new IOException("Source file is outside source root: " + sourcePath);
            }

            Path relativePath = sourceRoot.relativize(sourcePath);
            Path targetPath = targetRoot.resolve(relativePath).normalize();
            if (!targetPath.startsWith(targetRoot)) {
                throw new IOException("Resolved target file is outside target root: " + targetPath);
            }
            return targetPath.toFile();
        }

        private void transferOneFile(File sourceFile, File desFile) throws IOException {
            String fileName = sourceFile.getName().toLowerCase();
            if (fileName.endsWith("jpg") || fileName.endsWith("png") || !isCompress) {
                copyFileSafely(sourceFile, desFile);
                return;
            }
            handleCompression(sourceFile, desFile);
        }

        private void deleteSourceFile(File sourceFile) throws IOException {
            try {
                Files.delete(sourceFile.toPath());
            } catch (IOException e) {
                log.warn("Failed to delete source file after transfer: {}", sourceFile.getAbsolutePath(), e);
                throw e;
            }
        }

        private void recordFailure(File sourceFile, Exception e) {
            failedCount.incrementAndGet();
            failedFiles.add(sourceFile.getAbsolutePath() + " (" + e.getMessage() + ")");
        }

        private void handleCompression(File f1, File desFile) throws IOException {
            if ((filterSize != 0) && (f1.length() < filterSize * 1024)) {
                copyFileSafely(f1, desFile);
                return;
            }

            Path tempPath = createTempPath(desFile);
            try {
                log.info("开始压缩文件: {}", f1.getAbsolutePath());
                File tempFile = tempPath.toFile();
                if ("imageio".equals(compressMode)) {
                    TranscoderMain main = new TranscoderMain();
                    main.transcode(f1, tempFile, "JPEG2000Lossless");
                } else {
                    Dcm2Dcm main = new Dcm2Dcm();
                    main.setTransferSyntax(tanscode);
                    main.transcodeWithTranscoder(f1, tempFile);
                }
                commitGeneratedFile(tempPath, desFile);
                log.info("压缩完成: {}", f1.getAbsolutePath());
            } catch (Exception e) {
                deleteQuietly(tempPath);
                log.warn("压缩出错, 直接复制文件: {}", e.getMessage());
                copyFileSafely(f1, desFile);
            }
        }

        private void waitIfPaused() throws InterruptedException {
            synchronized (this) {
                while (pause || TransferTask.this.pause) {
                    log.info("暂停");
                    wait();
                }
            }
        }

        public void pause() {
            pauseTransfer();
        }

        public void pauseTransfer() {
            synchronized (this) {
                pause = true;
            }
        }

        public void resumeTransfer() {
            synchronized (this) {
                pause = false;
                notifyAll();
            }
        }
    }

    private void checkFile(File file, List<File> list, Pattern p) {
        if (p.matcher(file.getName()).matches()) {
            list.add(file);
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File f : files) {
                    checkFile(f, list, p);
                }
            }
        }
    }

    private void publishRuntimeMetrics(boolean force) {
        long now = System.currentTimeMillis();
        long last = lastUiUpdateTime.get();
        if (!force && now - last < 500) {
            return;
        }
        if (!lastUiUpdateTime.compareAndSet(last, now) && !force) {
            return;
        }

        long done = totalCount.get();
        long bytes = transferredBytes.get();
        long elapsedMillis = Math.max(1L, now - startTime);
        long bytesPerSecond = bytes * 1000L / elapsedMillis;
        String transferredFiles = count > 0 ? done + " / " + count : String.valueOf(done);
        String transferredSize = formatBytes(bytes);
        String speed = formatBytes(bytesPerSecond) + "/s";
        String elapsed = formatElapsed(elapsedMillis);
        String path = currentPath == null || currentPath.isEmpty() ? "-" : currentPath;
        String errorSkip = failedCount.get() + " / " + skippedCount.get();

        if (count > 0) {
            updateProgress(done, count);
            updateMessage(done + "/" + count);
        } else {
            updateProgress(-1, 1);
            updateMessage("已处理 " + done + " 个文件");
        }

        Platform.runLater(() -> {
            transferredFilesText.set(transferredFiles);
            transferredSizeText.set(transferredSize);
            speedText.set(speed);
            elapsedText.set(elapsed);
            currentPathText.set(path);
            errorSkipText.set(errorSkip);
        });
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        int unitIndex = -1;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format(java.util.Locale.ROOT, "%.2f %s", value, units[unitIndex]);
    }

    private String formatElapsed(long elapsedMillis) {
        long totalSeconds = elapsedMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }
}