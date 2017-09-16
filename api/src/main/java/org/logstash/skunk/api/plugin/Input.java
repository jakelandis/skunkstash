package org.logstash.skunk.api.plugin;

public interface Input {

    void start(InputConsumer consumer);

    void stop();

}
