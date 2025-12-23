package com.example.mngsys.redisevent;

/**
 * EventHandler。
 */
public interface EventHandler {
    boolean supports(String eventType);

    void handle(EventMessage message);
}
