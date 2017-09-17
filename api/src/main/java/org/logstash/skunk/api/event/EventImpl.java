package org.logstash.skunk.api.event;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventImpl implements Event {

    ConcurrentHashMap<Object, Object> values = new ConcurrentHashMap<>();
    LocalDateTime dateTime;

    @Override
    public <K, T> void addValue(K key, T value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    @Override
    public <K, T> T getValue(K key) {
        return (T) values.get(key);
    }

    @Override
    public <K, V> Map<K, V> getValues() {
        return (Map<K, V>) Collections.unmodifiableMap(values);
    }

    @Override
    public void setTimeStamp(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public LocalDateTime getTimeStamp() {
        return dateTime;
    }


}
