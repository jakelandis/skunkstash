package org.logstash.skunk.plugin.input;

import org.logstash.skunk.api.config.Configuration;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.WriteQueue;
import org.logstash.skunk.api.plugin.Input;
import org.logstash.skunk.api.plugin.LogStashPlugin;


@LogStashPlugin("generator")
public class Generator implements Input {

    @Override
    public void start(Configuration configuration, WriteQueue queue) {
        while(true){
            queue.put(new Event(){});
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
