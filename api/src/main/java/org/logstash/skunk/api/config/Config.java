package org.logstash.skunk.api.config;

import java.lang.annotation.*;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE})
public @interface Config {

    String value() default "";


    //"some deprecated message"
    String deprecated() default "";

    // "some obsolete message"
    String obsolete() default "";

    //
    boolean required() default false;

}
