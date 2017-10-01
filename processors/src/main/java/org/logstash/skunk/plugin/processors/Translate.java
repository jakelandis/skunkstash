package org.logstash.skunk.plugin.processors;


import org.logstash.skunk.api.config.Config;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventBatch;
import org.logstash.skunk.api.event.EventQueue;
import org.logstash.skunk.api.plugin.Obsoleted;
import org.logstash.skunk.api.plugin.Deprecated;
import org.logstash.skunk.api.plugin.Plugin;
import org.logstash.skunk.api.plugin.Processor;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

@Plugin("translate")
final public class Translate implements Processor {

    @Config("dictionary") private Map<String, Object> dictionary;
    @Config("dictionary_path") private String dictionaryPathAsString;
    @Config("destination") private String destination;
    @Config("exact") private boolean exact;
    @Config("override") private boolean override;
    @Config("regex") private boolean regex;
    @Config("fallback") private String fallBack;
    @Config("refresh_interval") private int refreshInterval;

    @Obsoleted("This value is no longer supported and will do nothing if set.")
    @Config("legacy") private boolean legacy;

    @Deprecated("This value is has been replaced by regex")
    @Config("regular_expression") private boolean regularExpression;


    private Path dictionaryPath;

    @Override
    public EventBatch process(EventBatch events) {
        EventQueue processedEventQueue = new EventQueue(new ArrayBlockingQueue<>(1024));
        while (events.hasNext()) {
            //Adding configuration to event only for illustrative purposes
            Event event = events.next();
            event.addValue("dictionary", dictionary);
            event.addValue("dictionary_path", dictionaryPath);
            event.addValue("refresh_interval", refreshInterval);
            event.addValue("destination", destination);
            event.addValue("exact", exact);
            event.addValue("override", override);
            event.addValue("fallback", fallBack);
            event.addValue("regex", regex);
            event.addValue("regular_expression", regularExpression);
            processedEventQueue.add(event);
        }
        return processedEventQueue;
    }

    @Override
    public void initialize() {
        System.out.println("Initializing...");

        //any transformations or validation of configuration should happen here
        dictionaryPath = new File(dictionaryPathAsString).toPath();
    }
}
