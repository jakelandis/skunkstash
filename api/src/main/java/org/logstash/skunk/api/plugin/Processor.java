package org.logstash.skunk.api.plugin;

import org.logstash.skunk.api.event.EventBatch;

public interface Processor {
    /**
     * Process events. In the past, this was called a `filters` in Logstash.
     *
     * @param events The events to be processed
     * @return Any new events created by this processor.
     */
    EventBatch process(EventBatch events);

    /**
     * This is guaranteed to be called after the configuration is fully populated
     */
    default void initialize(){
        //do nothing
    }
}