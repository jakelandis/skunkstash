package org.logstash.skunk.config;

import java.lang.annotation.*;

/**
 * TODO:
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Config {

    String value() default "";
}
