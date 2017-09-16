package org.logstash.skunk.api.plugin;

import org.logstash.skunk.api.event.EventBatch;

@FunctionalInterface
public interface Processor {
    /**
     * Process events. In the past, this was called a `filters` in Logstash.
     *
     * @param events The events to be processed
     * @return Any new events created by this processor.
     */
    EventBatch process(EventBatch events);
}