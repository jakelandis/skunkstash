package org.logstash.skunk;

import java.util.concurrent.ThreadFactory;

public class PluginThreadFactory implements ThreadFactory {

    private final String name;
    private final ClassLoader classLoader;

    public PluginThreadFactory(String name, ClassLoader classLoader) {
        this.name = name;
        this.classLoader = classLoader;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, name);
        t.setContextClassLoader(classLoader);
        return t;
    }
}
