package org.logstash.skunk.api.event;

import java.time.LocalDateTime;

public interface Event {


    //Only exists for something to pass around..not trying to re-define Event contract.



    LocalDateTime getTimestamp();
}
