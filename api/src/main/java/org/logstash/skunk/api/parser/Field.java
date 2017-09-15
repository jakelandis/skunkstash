package org.logstash.skunk.api.parser;


public interface Field {
    Field setDeprecated(String details);

    Field setObsolete(String details);
}
