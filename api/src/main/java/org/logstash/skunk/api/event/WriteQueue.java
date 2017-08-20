package org.logstash.skunk.api.event;

import java.util.Collection;

public interface WriteQueue {

    void put(Event event);

    void put(Collection<Event> events);

}
