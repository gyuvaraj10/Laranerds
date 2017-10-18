package com.app.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class PropertyMap {

    private static ThreadLocal<PropertyMap> map = new ThreadLocal<>();

    private final Properties properties;

    private PropertyMap() {
        properties = new Properties();
    }

    public Properties getProperties() {
        return properties;
    }

    public Properties getLoadedProperties() {
        try {
            ClassLoader loader = PropertyMap.class.getClassLoader();
            java.net.URL url = loader.getResource("application.properties");
            if(loader == null) {
                loader = ClassLoader.getSystemClassLoader();
                url = loader.getResource("application.properties");
            }
            InputStream stream = new FileInputStream(new File(url.getPath()));
            System.getProperties().load(stream);
            properties.putAll(System.getProperties());
            stream.close();
            return properties;
        } catch (IOException ex) {
            return properties;
        }
    }

    public static PropertyMap getInstance() {
        if(map.get() == null) {
            map.set(new PropertyMap());
        }
        return map.get();
    }
}
