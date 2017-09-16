package org.logstash.skunk.api.plugin;

import org.logstash.skunk.api.event.EventBatch;

public interface Output {

    void start();

    void stop();

    void stash(EventBatch events);
}
