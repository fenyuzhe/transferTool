package com.protoss.toolkit;

import javafx.application.Application;
import org.dcm4che3.data.UID;
import org.dcm4che3.tool.dcm2dcm.Dcm2Dcm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            File tempFile = new File(tempDir);
            if (!tempFile.exists() || !tempFile.canWrite()) {
                log.error("Temporary directory is not accessible: {}", tempDir);

                String os = System.getProperty("os.name").toLowerCase();

                String customTempDir;

                if (os.contains("win")) {
                    customTempDir = "C:/temp";
                } else {
                    // Linux / Unix
                    customTempDir = "/tmp/protoss-toolkit";
                }

                File customDir = new File(customTempDir);

                // 自动创建目录
                if (!customDir.exists()) {
                    boolean created = customDir.mkdirs();
                    log.info("Create custom temp dir: {}, result={}", customTempDir, created);
                }

                // 设置新的临时目录
                System.setProperty("java.io.tmpdir", customTempDir);

                log.info("Set custom temp directory: {}", customTempDir);
            }
            // 获取项目运行路径
            String projectPath = new File("").getAbsolutePath();
            log.info("Current project path: {}", projectPath);

            // 获取当前的 library path
            String existingPath = System.getProperty("java.library.path");

            // 组合新的 library path
            String newLibPath = projectPath + File.pathSeparator +
                    (existingPath != null ? existingPath : "");

            // 设置新的 java.library.path
            System.setProperty("java.library.path", newLibPath);
            log.info("Set java.library.path to: {}", newLibPath);

            // 如果需要加载特定的本地库，可以在这里显式加载
            System.loadLibrary("opencv_java");

            // 启动主应用程序
            log.info("Starting ToolkitApplication");
            ToolkitApplication.main(args);

        } catch (Exception e) {
            log.error("Failed to launch application", e);
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
