package com.example.mngsys.redisevent;

public interface EventHandler {
    boolean supports(String eventType);

    void handle(EventMessage message);
}
