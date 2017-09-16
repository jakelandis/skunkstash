package org.logstash.skunk.plugin.processors;

import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventBatch;
import org.logstash.skunk.api.event.EventQueue;
import org.logstash.skunk.api.plugin.Plugin;
import org.logstash.skunk.api.plugin.Processor;

import java.util.concurrent.ArrayBlockingQueue;


@Plugin("dropper")
public class Dropper implements Processor {

    private boolean toggle;


    @Override
    public EventBatch process(EventBatch events) {
        EventQueue processedEventQueue = new EventQueue(new ArrayBlockingQueue<>(1024));
        while (events.hasNext()) {

//            drops every other one
            toggle = !toggle;
            if (toggle) {
                processedEventQueue.add(events.next());
            }
        }
        return processedEventQueue;

    }


}