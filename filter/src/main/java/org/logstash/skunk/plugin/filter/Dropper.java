package org.logstash.skunk.plugin.filter;

import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.plugin.Filter;
import org.logstash.skunk.api.plugin.LogStashPlugin;

@LogStashPlugin("dropper")
public class Dropper implements Filter {

    private boolean toggle;

    @Override
    public Event filter(Event event) {
        //drops every other one
        toggle = !toggle;
        return toggle ? null: event;
    }
}
