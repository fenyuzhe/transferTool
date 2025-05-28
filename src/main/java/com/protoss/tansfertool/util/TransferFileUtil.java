package com.protoss.tansfertool.util;

import com.protoss.tansfertool.entity.DirEntry;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class TransferFileUtil {

    private static Logger log = LoggerFactory.getLogger(TransferFileUtil.class);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void copyFile(File srcFile, File desFile) {
        log.info(Thread.currentThread().getName() + "---复制文件:" + srcFile.getAbsolutePath() + "===>" + desFile.getAbsolutePath());
        if (!desFile.getParentFile().exists()) {
            desFile.getParentFile().mkdirs();
        }
        try (FileInputStream fis = new FileInputStream(srcFile);
             FileOutputStream fos = new FileOutputStream(desFile);
             FileChannel in = fis.getChannel();
             FileChannel out = fos.getChannel()) {
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            log.error("Error copying file", e);
        }
    }

    public static void getDir(File srcFile, List<DirEntry> dirList, String regex, LocalDate start_date, LocalDate end_date) {
        if (srcFile.listFiles() == null) {
            return;
        }
        Pattern p = Pattern.compile(regex);
        for (File item : srcFile.listFiles()) {
            if (item.isDirectory() && p.matcher(item.getName()).matches()) {
                if ((LocalDate.parse(item.getName(), formatter).isAfter(start_date) || LocalDate.parse(item.getName(), formatter).isEqual(start_date)) &&
                        (LocalDate.parse(item.getName(), formatter).isBefore(end_date) || LocalDate.parse(item.getName(), formatter).isEqual(end_date))) {
                    DirEntry entry = new DirEntry();
                    entry.setDirName(item.getName());
                    entry.setDirPath(item.getAbsolutePath());
                    dirList.add(entry);
                }
            } else {
                getDir(item, dirList, regex, start_date, end_date);
            }
        }
    }
}