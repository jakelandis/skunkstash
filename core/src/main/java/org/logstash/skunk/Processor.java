package org.logstash.skunk;

import org.logstash.skunk.api.config.Configuration;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.plugin.Filter;
import org.logstash.skunk.api.plugin.Output;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class Processor {
    private final List<Filter> filters;
    private final List<Output> output;
    private final Configuration configuration;
    private final Queue<Event> queue;

    public Processor(Configuration configuration, List<Filter> filters, List<Output> output, Queue<Event> queue) {
        this.filters = filters;
        this.output = output;
        this.configuration = configuration;
        this.queue = queue;
    }

    public void start(){
        output.forEach(o -> o.start(configuration));
        while(true){
            Event event = queue.peek();
            filter(event, filters.iterator());
            if(event != null){
                output.forEach(o -> o.stash(event));
            }
            queue.poll();
        }
    }

    private Event filter(Event event, Iterator<Filter> it){
        if(event != null && it.hasNext()){
            filter(it.next().filter(event), it);
        }
        return event;
    }

}
