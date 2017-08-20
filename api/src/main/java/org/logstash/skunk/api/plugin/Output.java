package org.logstash.skunk.api.plugin;

import org.logstash.skunk.api.config.Configuration;
import org.logstash.skunk.api.event.Event;

public interface Output {

    void start(Configuration configuration);

    void stop();

    void stash(Event event);

    boolean isRunning();
}
