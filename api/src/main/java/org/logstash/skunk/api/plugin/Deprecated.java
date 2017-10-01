package org.logstash.skunk.api.plugin;

import java.lang.annotation.*;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Deprecated {

    String value() default "";

}
