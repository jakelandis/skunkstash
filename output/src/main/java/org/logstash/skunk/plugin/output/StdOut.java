package org.logstash.skunk.plugin.output;

import org.logstash.skunk.api.config.Configuration;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.plugin.Filter;
import org.logstash.skunk.api.plugin.LogStashPlugin;
import org.logstash.skunk.api.plugin.Output;

import java.util.List;

@LogStashPlugin("stdout")
public class StdOut implements Output {


    boolean running;

    @Override
    public void start(Configuration configuration) {
        running = true;
    }

    @Override
    public void stop() {
        if (isRunning()) {
            System.out.println("Stopping stdout");
            running = false;
        }
    }

    @Override
    public void stash(Event event) {
        System.out.println(event.hashCode());
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
