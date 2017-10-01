package org.logstash.skunk.api.validate;

public interface Validatable {

    void validate() throws ValidationException;

}
