package org.logstash.skunk;

import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventQueue;
import org.logstash.skunk.api.plugin.InputConsumer;

import java.util.Collection;

public class EventQueueInputConsumer implements InputConsumer{

    public final EventQueue eventQueue;

    public EventQueueInputConsumer(EventQueue eventQueue) {
        this.eventQueue = eventQueue;
    }

    @Override
    public void accept(Collection<Event> events) {
        eventQueue.add(events);
    }
}
