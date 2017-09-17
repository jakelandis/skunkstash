package org.logstash.skunk.api.event;

import java.time.LocalDateTime;
import java.util.Map;

//not trying to re-define Event...just need something to pass around in skunkstash
public interface Event {

    <K, V> void addValue(K key, V value);

    <K, V> V getValue(K key);

    <K, V> Map<K, V> getValues();

    void setTimeStamp(LocalDateTime dateTime);

    LocalDateTime getTimeStamp();

}
