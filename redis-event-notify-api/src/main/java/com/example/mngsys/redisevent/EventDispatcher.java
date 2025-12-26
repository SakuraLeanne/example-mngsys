package com.example.mngsys.redisevent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * EventDispatcher。
 */
public class EventDispatcher {
    private final List<EventHandler> handlers;

    public EventDispatcher(List<EventHandler> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            this.handlers = Collections.emptyList();
            return;
        }
        this.handlers = Collections.unmodifiableList(new ArrayList<>(handlers));
    }

    public boolean handle(EventMessage message) {
        if (message == null) {
            return false;
        }
        String eventType = message.getEventType();
        boolean handled = false;
        for (EventHandler handler : handlers) {
            if (!handler.supports(eventType)) {
                continue;
            }
            try {
                handler.handle(message);
                handled = true;
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Failed to handle eventType=" + Objects.toString(eventType, "<null>"),
                        ex
                );
            }
        }
        return handled;
    }
}
