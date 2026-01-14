package com.protoss.tansfertool.thread;

import com.protoss.tansfertool.codec.TranscoderMain;
import javafx.concurrent.Task;
import org.dcm4che3.tool.dcm2dcm.Dcm2Dcm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static com.protoss.tansfertool.util.TransferFileUtil.copyFile;

public class TransferTask extends Task<Integer> {
    private static Logger log = LoggerFactory.getLogger(TransferTask.class);
    final private AtomicLong totalCount = new AtomicLong();
    private boolean pause = false;
    private List<File> sourceFileList;
    private String sourceFilePath;
    private String targetFile;
    private String strategy;
    private boolean isCompress;
    private long filterSize;
    private int threads;
    private String tanscode;
    private Long count;
    private List<TransferThread> threadList;
    private String compressMode;

    private String pattern = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})(((0[13578]|1[02])(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)(0[1-9]|[12][0-9]|30))|(02(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))0229)";

    public TransferTask(String compressMode, Long count, List<File> sourceFileList, String sourceFilePath,
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
    protected Integer call() {
        long start_time = System.currentTimeMillis();
        List<File> list = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);
        for (File f : sourceFileList) {
            checkFile(f, list, p);
        }
        log.info("待转移文件夹:" + list + "===list大小:" + list.size());
        threadList = new ArrayList<>();
        if (list.size() > threads) {
            int perThreadCount = list.size() / threads; // 每个线程处理的文件夹数
            int remainCount = list.size() % threads; // 处理不完的文件夹数
            int startIndex = 0; // 每个线程开始处理的文件夹数下标
            for (int i = 0; i < threads; i++) {
                int endIndex = (i < remainCount) ? startIndex + perThreadCount + 1 : startIndex + perThreadCount;
                List<File> subList = list.subList(startIndex, endIndex);
                log.info(i + "===subList:" + subList + "===subList大小:" + subList.size());
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
                log.info(thread.getName() + "-单独线程处理: " + subList);
                threadList.add(thread);
                thread.start();
            }
        }
        for (TransferThread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.error("Thread interrupted", e);
            }
        }
        log.info("耗时：" + (System.currentTimeMillis() - start_time));

        // 如果是移动策略，且任务顺利完成，进行空文件夹清理
        if ("移动".equals(strategy)) {
            log.info("开始清理空文件夹...");
            deleteEmptyDirs(new File(sourceFilePath));
        }

        return 1;
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
                    log.info("Deleted empty directory: " + dir.getAbsolutePath());
                } else {
                    log.warn("Failed to delete empty directory: " + dir.getAbsolutePath());
                }
            }
        }
    }

    public void pause() {
        pause = true;
        for (TransferThread thread : threadList) {
            thread.pause();
        }
    }

    public void resume() {
        pause = false;
        for (TransferThread thread : threadList) {
            thread.resume1();
        }
    }

    class TransferThread extends Thread {
        private boolean pause = false;
        private List<File> sourceFileList;
        private String sourceFilePath;
        private String targetFile;
        private String strategy;
        private boolean isCompress;
        private long filterSize;
        private String tanscode;
        private String compressMode;

        public TransferThread(List<File> sourceFileList, String sourceFilePath, String targetFile, String strategy,
                boolean isCompress, String tanscode, long filterSize, String compressMode) {
            this.sourceFileList = sourceFileList;
            this.sourceFilePath = sourceFilePath;
            this.targetFile = targetFile;
            this.strategy = strategy;
            this.isCompress = isCompress;
            this.filterSize = filterSize;
            this.tanscode = tanscode;
            this.compressMode = compressMode;
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
                    while (pause) {
                        synchronized (this) {
                            try {
                                log.info("暂停");
                                wait();
                            } catch (InterruptedException e) {
                                log.error("线程中断", e);
                            }
                        }
                    }

                    if (f1.isFile()) {
                        try {
                            File desFile = new File(f1.getAbsolutePath().replace(sourceFilePath, targetFile));
                            if ((f1.getName().toLowerCase().endsWith("jpg"))
                                    || (f1.getName().toLowerCase().endsWith("png"))) {
                                copyFile(f1, desFile);
                            } else if (isCompress) {
                                handleCompression(f1, desFile);
                            } else {
                                copyFile(f1, desFile);
                            }

                            if (desFile.exists()) {
                                if ("移动".equals(strategy)) {
                                    f1.delete();
                                }
                                totalCount.incrementAndGet();
                                updateProgress(totalCount.longValue(), count);
                                updateMessage(totalCount.longValue() + "/" + count);
                            }
                        } catch (Exception e) {
                            log.error("Error transferring file: " + f1.getAbsolutePath(), e);
                        }
                    } else {
                        transfer(f1);
                    }
                }
            }

            if ("移动".equals(strategy) && f.isDirectory()) {
                System.out.println(f.getAbsolutePath());
                File[] remainingFiles = f.listFiles();
                if (remainingFiles == null || remainingFiles.length == 0) {
                    System.out.println(f.getAbsolutePath() + "是空文件夹，执行删除");
                    if (!f.delete()) {
                        log.error("Failed to delete folder: " + f.getAbsolutePath());
                    }
                }
            }
        }

        private void handleCompression(File f1, File desFile) {
            File parentDir = desFile.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    log.error("Failed to create directory: " + parentDir.getAbsolutePath());
                    copyFile(f1, desFile);
                    return;
                }
            }
            try {
                log.info("开始压缩文件: {}", f1.getAbsolutePath());
                if (compressMode.equals("imageio")) {
                    TranscoderMain main = new TranscoderMain();
                    main.transcode(f1, desFile, "JPEG2000Lossless");
                } else {
                    Dcm2Dcm main = new Dcm2Dcm();
                    main.setTransferSyntax(tanscode);
                    main.transcodeWithTranscoder(f1, desFile);
                }
                log.info("压缩完成: {}", f1.getAbsolutePath());

                if (((filterSize != 0) && (f1.length() < filterSize * 1024)) || !desFile.exists()) {
                    copyFile(f1, desFile);
                }
            } catch (Exception e) {
                log.warn("压缩出错, 直接复制文件: " + e.getMessage());
                copyFile(f1, desFile);
            }
        }

        public void pause() {
            pause = true;
        }

        public void resume1() {
            pause = false;
            synchronized (this) {
                notify();
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
}
