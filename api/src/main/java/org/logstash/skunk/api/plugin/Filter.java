package org.logstash.skunk.api.plugin;

import org.logstash.skunk.api.event.Event;

@FunctionalInterface
public interface Filter {
    Event filter(Event event);
}
