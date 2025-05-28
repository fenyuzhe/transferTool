package com.protoss.tansfertool.dcm4cheri;

public class MyConfigurationException extends RuntimeException{

    MyConfigurationException(String msg) {
        super(msg);
    }

    MyConfigurationException(String msg, Exception x) {
        super(msg, x);
    }
}
