package org.logstash.skunk.api.event;

import java.util.Collection;
import java.util.Queue;

public class EventQueue implements EventBatch {

    private final Queue<Event> queue;


    public EventQueue(Queue<Event> queue) {
        this.queue = queue;
    }

    @Override
    public Event next() {
        return queue.poll();
    }

    @Override
    public boolean hasNext() {
        return queue.peek() != null;
    }

    public void add(Collection<Event> events){
        queue.addAll(events);
    }

    public void add(Event event){
        queue.add(event);
    }

}
