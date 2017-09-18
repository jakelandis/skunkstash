package experimental;


import org.logstash.skunk.api.event.Event;
import org.logstash.skunk.api.event.EventBatch;
import org.logstash.skunk.api.event.EventQueue;
import org.logstash.skunk.api.plugin.Plugin;
import org.logstash.skunk.api.plugin.Processor;

import java.util.concurrent.ArrayBlockingQueue;

@Plugin("translate")
public class Translate implements Processor {


    private final TranslateConfig config;

    public Translate(TranslateConfig config){
        this.config = config;
    }


    @Override
    public EventBatch process(EventBatch events) {
        EventQueue processedEventQueue = new EventQueue(new ArrayBlockingQueue<>(1024));
        while (events.hasNext()) {
            //Adding configuration to event only for illustrative purposes
            Event event = events.next();
            event.addValue("dictionary", config.getDictionary());
            event.addValue("dictionary_path", config.getDictionaryPath());
            event.addValue("refresh_interval", config.getRefreshInterval());
            event.addValue("destination", config.getDestination());
            event.addValue("exact", config.isExact());
            event.addValue("override", config.isOverride());
            event.addValue("fallback", config.getFallBack());
            processedEventQueue.add(event);

        }
        return processedEventQueue;

    }
}
