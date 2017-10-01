package org.logstash.skunk.api.config;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Config {

    String value() default "";



    boolean required() default false;


}
