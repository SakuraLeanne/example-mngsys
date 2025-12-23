package com.example.mngsys.redisevent;

public interface EventMessageHandler {
    boolean handle(EventMessage message);
}
