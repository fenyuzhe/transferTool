package com.protoss.tansfertool.dcm4cheri;

import com.sun.media.imageioimpl.plugins.jpeg2000.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.util.Iterator;
import java.util.Properties;

public class MyImageWriterFactory {
    private static Logger log = LoggerFactory.getLogger(MyImageWriterFactory.class);
    private static final MyImageWriterFactory instance = new MyImageWriterFactory();
    private final Properties map = new Properties();

    public static final MyImageWriterFactory getInstance() {
        return instance;
    }

    private MyImageWriterFactory() {
        MyConfigurationUtils.loadPropertiesForClass(this.map, MyImageWriterFactory.class);
    }

    public synchronized ImageWriter getWriterForTransferSyntax(String tsuid) {
        String s = this.map.getProperty(tsuid);
        if (s == null) {
            throw new UnsupportedOperationException("No Image Writer available for Transfer Syntax:" + tsuid);
        } else {
            int delim = s.indexOf(44);
            if (delim == -1) {
                throw new MyConfigurationException("Missing ',' in " + tsuid + "=" + s);
            } else {
                String formatName = s.substring(0, delim);
                String className = s.substring(delim + 1);
                log.info("formatName:"+formatName);
                log.info("className:"+className);
                Iterator it = ImageIO.getImageWritersByFormatName(formatName);
                J2KImageWriterSpi spi = new J2KImageWriterSpi();
                J2KImageWriter r = new J2KImageWriter(spi);
//                while (it.hasNext()) {
//                    J2KImageWriter iw = (J2KImageWriter) it.next();
//                    log.info("ImageWriter:"+iw.getClass());
//                    if (iw instanceof com.sun.media.imageioimpl.plugins.jpeg2000.J2KImageWriter) {
//                        r = iw;
//                        break;
//                    }
//                }
                return r;
            }
        }
    }
}
