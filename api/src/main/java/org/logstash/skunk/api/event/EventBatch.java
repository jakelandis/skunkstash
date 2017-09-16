package org.logstash.skunk.api.event;


import java.util.Iterator;


public interface EventBatch extends Iterator<Event> {

    @Override
    Event next();

    @Override
    boolean hasNext();
}
