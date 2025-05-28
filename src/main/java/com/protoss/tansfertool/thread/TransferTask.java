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
    private String compressMode; // 添加 compressMode 字段

    private String pattern = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})(((0[13578]|1[02])(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)(0[1-9]|[12][0-9]|30))|(02(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))0229)";

    public TransferTask(String compressMode, Long count, List<File> sourceFileList, String sourceFilePath,
                        String targetFile, String strategy, boolean isCompress, String tanscode, long filterSize, int threads) {
        this.compressMode = compressMode; // 初始化 compressMode
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
            if (p.matcher(f.getName()).matches()) {
                Collections.addAll(list, f);
            } else {
                list = sourceFileList;
            }
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
                TransferThread thread = new TransferThread(subList, sourceFilePath, targetFile, strategy, isCompress, tanscode, filterSize, compressMode);
                threadList.add(thread);
                startIndex = endIndex;
                thread.start(); // 启动线程
            }
        } else {
            for (File file : list) {
                List<File> subList = Collections.singletonList(file);
                TransferThread thread = new TransferThread(subList, sourceFilePath, targetFile, strategy, isCompress, tanscode, filterSize, compressMode);
                log.info(thread.getName()+"-单独线程处理: " + subList);
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
        return 1;
    }

    public void pause() {
        pause = true;
        for (TransferThread thread : threadList) {
            thread.pause(); // 暂停所有线程
        }
    }

    public void resume() {
        pause = false;
        for (TransferThread thread : threadList) {
            thread.resume1(); // 恢复所有线程
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
        private String compressMode; // 添加 compressMode 字段

        public TransferThread(List<File> sourceFileList, String sourceFilePath, String targetFile, String strategy, boolean isCompress, String tanscode, long filterSize, String compressMode) {
            this.sourceFileList = sourceFileList;
            this.sourceFilePath = sourceFilePath;
            this.targetFile = targetFile;
            this.strategy = strategy;
            this.isCompress = isCompress;
            this.filterSize = filterSize;
            this.tanscode = tanscode;
            this.compressMode = compressMode; // 初始化 compressMode
        }

        public void run() {
            for (File sourceFile : sourceFileList) {
                transfer(sourceFile);
            }
        }

        public void transfer(File f) {
            File[] files = f.listFiles();
            // 先处理子文件和子文件夹
            if (files != null && files.length > 0) {
                for (final File f1 : files) {
                    while (pause) {
                        synchronized (this) {
                            try {
                                log.info("暂停");
                                wait();
                            } catch (InterruptedException e) {
                                log.error("Thread interrupted while waiting", e);
                            }
                        }
                    }

                    if (f1.isFile()) {
                        try {
                            File desFile = new File(f1.getAbsolutePath().replace(sourceFilePath, targetFile));
                            if ((f1.getName().toLowerCase().endsWith("jpg")) || (f1.getName().toLowerCase().endsWith("png"))) {
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
                        transfer(f1);  // 递归处理子文件夹
                    }
                }
            }

            // 在处理完所有子内容后，检查文件夹是否为空并删除
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
            if (!desFile.getParentFile().exists()) {
                desFile.getParentFile().mkdirs();
            }
            try {
                if (compressMode.equals("imageio")) {
                    TranscoderMain main = new TranscoderMain();
                    main.transcode(f1, desFile, "JPEG2000Lossless");
                } else {
                    Dcm2Dcm main = new Dcm2Dcm();
                    main.setTransferSyntax(tanscode);
                    main.transcodeWithTranscoder(f1, desFile);
                }

                if (((filterSize != 0) && (f1.length() < filterSize * 1024)) || !desFile.exists()) {
                    copyFile(f1, desFile);
                }
            } catch (Exception e) {
                log.error("Error during compression", e);
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
}
