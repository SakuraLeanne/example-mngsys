package com.example.mngsys.redisevent;

/**
 * EventPublisher。
 */
public interface EventPublisher {
    String publish(String streamKey, EventMessage message);

    default String publish(EventMessage message) {
        return publish(null, message);
    }
}
