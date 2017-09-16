package org.logstash.skunk;

import org.logstash.skunk.api.event.EventBatch;
import org.logstash.skunk.api.plugin.Output;
import org.logstash.skunk.api.plugin.Processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// the thing that runs the processors and outputs (name borrowed from Flume)
public class Sink {
    private final List<Processor> processors;
    private final List<Output> outputs;
    private final EventBatch events;

    boolean run;


    public Sink(List<Processor> processors, List<Output> outputs, EventBatch events) {
        this.processors = processors;
        this.outputs = outputs;
        this.events = events;
    }

    public void start() {
        run = true;
        //start the outputs
        outputs.forEach(o -> o.start());
        while (run) {

            EventBatch proceededBatch = events;
            Iterator<Processor> it = processors.iterator();
            if (!processors.isEmpty()) {
                proceededBatch = it.next().process(events);
                while (it.hasNext()) {
                    proceededBatch = it.next().process(proceededBatch);
                }
            }

            for (Output output : outputs) {
                output.stash(proceededBatch);
            }
        }
    }


    public void stop() {
        System.out.println("Stopping processor");
        outputs.stream().forEach(o -> o.stop());
        run = false;
    }

}
