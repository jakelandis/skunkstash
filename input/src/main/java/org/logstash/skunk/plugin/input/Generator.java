package org.logstash.skunk.plugin.input;

import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.plugin.Input;
import org.logstash.skunk.api.plugin.InputConsumer;
import org.logstash.skunk.api.plugin.Plugin;

import java.time.LocalDateTime;
import java.util.Collections;


@Plugin("generator")
public class Generator implements Input {

    boolean running;

    @Override
    public void start(InputConsumer consumer) {
        running = true;
        while (running) {
            consumer.accept(Collections.singleton(new Event(){
                private LocalDateTime dateTime = LocalDateTime.now();

                @Override
                public LocalDateTime getTimestamp() {
                    return dateTime;
                }
            }));
            try {
                Thread.sleep(1000);
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
