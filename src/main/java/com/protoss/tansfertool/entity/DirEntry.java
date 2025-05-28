package com.protoss.tansfertool.entity;


public class DirEntry implements Comparable<DirEntry>{
    private String dirName;
    private String dirPath;

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    @Override
    public int compareTo(DirEntry o) {
        return dirName.compareTo(o.dirName);
    }
}
