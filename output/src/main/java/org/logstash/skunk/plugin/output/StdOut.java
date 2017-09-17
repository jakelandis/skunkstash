package org.logstash.skunk.plugin.output;

import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventBatch;
import org.logstash.skunk.api.plugin.Output;
import org.logstash.skunk.api.plugin.Plugin;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Plugin("stdout")
public class StdOut implements Output {


    boolean running;

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        if (running) {
            System.out.println("Stopping stdout");
            running = false;
        }
    }

    @Override
    public void stash(EventBatch events) {
        if (events != null) {
            while (events.hasNext()) {
                System.out.println("********************");
                Event event = events.next();
                System.out.println(formatter.format(event.getTimeStamp()));
                for (Map.Entry<Object, Object> entry : event.getValues().entrySet()) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    System.out.println(key + " :: " + value);
                }
            }
        }
    }

}
