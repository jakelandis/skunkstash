package org.logstash.skunk.api.config;

import java.lang.annotation.*;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Config {

    String value() default "";
}
