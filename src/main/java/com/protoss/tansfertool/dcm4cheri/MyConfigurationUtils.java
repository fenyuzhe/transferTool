package com.protoss.tansfertool.dcm4cheri;

import org.dcm4che.util.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class MyConfigurationUtils {
    MyConfigurationUtils() {
    }

    public static void loadPropertiesForClass(Properties map, Class c)  {
        String key = c.getName();
        String val = SystemUtils.getSystemProperty(key, (String)null);
        URL url;
        if (val == null) {
            val = key.replace('.', '/') + ".properties";
            url = getResource(c, val);
        } else {
            try {
                url = new URL(val);
            } catch (MalformedURLException var12) {
                url = getResource(c, val);
            }
        }

        try {
            InputStream is = url.openStream();

            try {
                map.load(is);
            } finally {
                is.close();
            }

        } catch (IOException var11) {
            throw new MyConfigurationException("failed not load resource:", var11);
        }
    }

    private static URL getResource(Class c, String val)  {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url;
        if ((cl == null || (url = cl.getResource(val)) == null) && (url = c.getClassLoader().getResource(val)) == null) {
            throw new MyConfigurationException("missing resource: " + val);
        } else {
            return url;
        }
    }
}
