package org.logstash.skunk.plugin.input;

import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventImpl;
import org.logstash.skunk.api.plugin.Input;
import org.logstash.skunk.api.plugin.InputConsumer;
import org.logstash.skunk.api.plugin.Plugin;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;


@Plugin("generator")
public class Generator implements Input {

    boolean running;

    @Override
    public void start(InputConsumer consumer) {
        running = true;
        while (running) {
            Event event = new EventImpl();
            event.setTimeStamp(LocalDateTime.now());
            event.addValue("message", UUID.randomUUID().toString());
            consumer.accept(Collections.singleton(event));
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if(running){
            System.out.println("Stopping generator");
            running = false;
        }
    }
}
