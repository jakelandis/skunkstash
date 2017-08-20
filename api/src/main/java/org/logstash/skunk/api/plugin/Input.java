package org.logstash.skunk.api.plugin;

import org.logstash.skunk.api.config.Configuration;
import org.logstash.skunk.api.event.WriteQueue;

public interface Input {

    void start(Configuration configuration, WriteQueue queue);

    void stop();

    boolean isRunning();
}
