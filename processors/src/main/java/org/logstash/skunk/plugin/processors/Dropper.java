package org.logstash.skunk.plugin.processors;

import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventBatch;
import org.logstash.skunk.api.event.EventQueue;
import org.logstash.skunk.api.plugin.Plugin;
import org.logstash.skunk.api.plugin.Processor;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Drops every other event
 */
@Plugin("dropper")
public class Dropper implements Processor {

    private boolean toggle;


    @Override
    public EventBatch process(EventBatch events) {
        EventQueue processedEventQueue = new EventQueue(new ArrayBlockingQueue<>(1024));
        while (events.hasNext()) {
            toggle = !toggle;
            Event event = events.next();
            if (toggle) {
                processedEventQueue.add(event);
            }
        }
        return processedEventQueue;
    }
}