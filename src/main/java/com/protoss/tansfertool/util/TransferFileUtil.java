package com.protoss.tansfertool.util;

import com.protoss.tansfertool.entity.DirEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class TransferFileUtil {

    private static Logger log = LoggerFactory.getLogger(TransferFileUtil.class);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String[] SKIP_SCAN_DIR_KEYWORDS = {"report", "import", "ncpacsris", "未授权图像"};

    public static void copyFile(File srcFile, File desFile) throws IOException {
        copyFileSafely(srcFile, desFile);
    }

    public static void copyFileSafely(File srcFile, File desFile) throws IOException {
        log.info("{}---复制文件: {} ===> {}", Thread.currentThread().getName(), srcFile.getAbsolutePath(), desFile.getAbsolutePath());

        Path sourcePath = srcFile.toPath().toAbsolutePath().normalize();
        Path targetPath = desFile.toPath().toAbsolutePath().normalize();
        Path parentPath = targetPath.getParent();
        if (parentPath == null) {
            throw new IOException("Target file has no parent directory: " + targetPath);
        }

        Files.createDirectories(parentPath);

        Path tempPath = buildTempPath(targetPath);
        try {
            Files.copy(sourcePath, tempPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            long sourceSize = Files.size(sourcePath);
            long tempSize = Files.size(tempPath);
            if (sourceSize != tempSize) {
                throw new IOException("Copied file size mismatch: source=" + sourceSize + ", target=" + tempSize
                        + ", file=" + sourcePath);
            }
            moveReplacing(tempPath, targetPath);
        } catch (IOException e) {
            deleteQuietly(tempPath);
            throw e;
        }
    }

    /**
     * 在目标文件的同级目录下创建临时路径，并确保父目录存在。
     * 用于先写临时文件再原子替换的场景（如压缩转码）。
     */
    public static Path createTempPath(File targetFile) throws IOException {
        Path targetPath = targetFile.toPath().toAbsolutePath().normalize();
        Path parentPath = targetPath.getParent();
        if (parentPath == null) {
            throw new IOException("Target file has no parent directory: " + targetPath);
        }
        Files.createDirectories(parentPath);
        return buildTempPath(targetPath);
    }

    public static void commitGeneratedFile(Path tempPath, File targetFile) throws IOException {
        Path targetPath = targetFile.toPath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(tempPath)) {
            throw new IOException("Generated file does not exist: " + tempPath);
        }
        long tempSize = Files.size(tempPath);
        if (tempSize <= 0) {
            throw new IOException("Generated file is empty: " + tempPath);
        }
        moveReplacing(tempPath, targetPath);
    }

    public static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temporary file: {}", path, e);
        }
    }

    /** 构造临时文件路径（与目标文件同目录，以 . 开头并附带随机 UUID 后缀） */
    private static Path buildTempPath(Path targetPath) {
        String fileName = targetPath.getFileName().toString();
        return targetPath.resolveSibling("." + fileName + ".tmp-" + UUID.randomUUID());
    }

    private static void moveReplacing(Path sourcePath, Path targetPath) throws IOException {
        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void getDir(File srcFile, List<DirEntry> dirList, String regex, LocalDate start_date, LocalDate end_date) {
        getDir(srcFile, dirList, Pattern.compile(regex), start_date, end_date);
    }

    private static void getDir(File srcFile, List<DirEntry> dirList, Pattern pattern, LocalDate start_date, LocalDate end_date) {
        if (shouldSkipDateDirScan(srcFile)) {
            return;
        }

        // 只调用一次 listFiles()，避免 TOCTOU 竞态导致的 NullPointerException
        File[] files = srcFile.listFiles();
        if (files == null) {
            return;
        }

        boolean hasDateDir = false;
        for (File item : files) {
            if (!item.isDirectory()) {
                continue;
            }
            log.info("扫描路径: {}", item.getAbsolutePath());
            if (pattern.matcher(item.getName()).matches()) {
                hasDateDir = true;
                // 只解析一次日期，避免重复计算
                LocalDate itemDate = LocalDate.parse(item.getName(), formatter);
                if (!itemDate.isBefore(start_date) && !itemDate.isAfter(end_date)) {
                    DirEntry entry = new DirEntry();
                    entry.setDirName(item.getName());
                    entry.setDirPath(item.getAbsolutePath());
                    dirList.add(entry);
                }
            }
        }

        if (hasDateDir) {
            return;
        }

        for (File item : files) {
            if (item.isDirectory()) {
                getDir(item, dirList, pattern, start_date, end_date);
            }
        }
    }

    private static boolean shouldSkipDateDirScan(File dir) {
        if (dir == null || dir.getName() == null) {
            return false;
        }
        String dirName = dir.getName().toLowerCase(Locale.ROOT);
        for (String keyword : SKIP_SCAN_DIR_KEYWORDS) {
            if (dirName.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
