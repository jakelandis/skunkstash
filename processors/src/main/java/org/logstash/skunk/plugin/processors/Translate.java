package org.logstash.skunk.plugin.processors;

import org.logstash.skunk.api.config.Config;
import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventBatch;
import org.logstash.skunk.api.event.EventQueue;
import org.logstash.skunk.api.plugin.Plugin;
import org.logstash.skunk.api.plugin.Processor;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

@Plugin("translate")
public class Translate implements Processor {


    private final Map<String, Object> dictionary;
    private final Path dictionaryPath;
    private final String destination;
    private final boolean exact;
    private final boolean override;
    private final boolean regex;
    private final String fallBack;
    private final int refreshInterval;


    public Translate(@Config("dictionary") Map<String, Object> dictionary,
                     @Config("destination") String destination,
                     @Config("exact") boolean exact,
                     @Config("override") boolean override,
                     @Config("regex") boolean regex,
                     @Config("fallback") String fallBack) {
        this.dictionary = dictionary;
        this.dictionaryPath = null;
        this.destination = destination;
        this.exact = exact;
        this.override = override;
        this.regex = regex;
        this.fallBack = fallBack;
        this.refreshInterval = -1;
    }

    public Translate(@Config("dictionary_path") Path dictionaryPath,
                     @Config("refresh_interval") int refreshInterval,
                     @Config("destination") String destination,
                     @Config("exact") boolean exact,
                     @Config("override") boolean override,
                     @Config("regex") boolean regex,
                     @Config("fallback") String fallBack) {
        this.dictionary = null;
        this.dictionaryPath = dictionaryPath;
        this.refreshInterval = refreshInterval;
        this.destination = destination;
        this.exact = exact;
        this.override = override;
        this.regex = regex;
        this.fallBack = fallBack;
    }


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
            processedEventQueue.add(event);

        }
        return processedEventQueue;

    }
}
