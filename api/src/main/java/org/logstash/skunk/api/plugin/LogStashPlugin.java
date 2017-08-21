package org.logstash.skunk.api.plugin;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LogStashPlugin {
    /**
     * The name of the plugin
     */
    String value();

    /**
     * Declares if there should be at most 1 single instance. Plugins that declare this to be true MUST be thread safe.
     */
    boolean singleton() default false;
}