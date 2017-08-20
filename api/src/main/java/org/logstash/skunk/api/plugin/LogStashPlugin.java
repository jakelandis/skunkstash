package org.logstash.skunk.api.plugin;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LogStashPlugin {
    String value();
}