package org.logstash.skunk.api.plugin;

import org.logstash.skunk.api.event.Event;

import java.util.Collection;
import java.util.function.Consumer;

public interface InputConsumer extends Consumer<Collection<Event>> {
}
