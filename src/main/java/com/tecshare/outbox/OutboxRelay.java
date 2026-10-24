package com.tecshare.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class OutboxRelay {
    public enum Status { PENDING, PROCESSING, SENT, DEAD }

    public record Event(String eventId, int attempts, Status status,
                        Instant availableAt, String lastError) {
        Event retryAt(Instant next, String error) {
            int nextAttempt = attempts + 1;
            Status nextStatus = nextAttempt >= 3 ? Status.DEAD : Status.PENDING;
            return new Event(eventId, nextAttempt, nextStatus, next, error);
        }
        Event sent() { return new Event(eventId, attempts, Status.SENT, availableAt, null); }
    }

    public interface Publisher { void publish(Event event) throws Exception; }
    public interface Consumer { boolean applyOnce(String eventId); }

    private final Publisher publisher;
    private final List<Event> events = new ArrayList<>();

    public OutboxRelay(Publisher publisher) { this.publisher = publisher; }

    public void add(Event event) { events.add(event); }

    public List<Event> poll(Instant now) {
        List<Event> snapshot = new ArrayList<>(events);
        for (int i = 0; i < snapshot.size(); i++) {
            Event event = snapshot.get(i);
            if (event.status() != Status.PENDING || event.availableAt().isAfter(now)) continue;
            Event processing = new Event(event.eventId(), event.attempts(), Status.PROCESSING,
                    event.availableAt(), event.lastError());
            try {
                publisher.publish(processing);
                events.set(i, processing.sent());
            } catch (Exception failure) {
                long delaySeconds = Math.min(60, 1L << Math.min(processing.attempts() + 1, 6));
                events.set(i, processing.retryAt(now.plusSeconds(delaySeconds), failure.getMessage()));
            }
        }
        return List.copyOf(events);
    }

    public static Duration retryDelay(int attempt) {
        return Duration.ofSeconds(Math.min(900, 1L << Math.min(attempt, 10)));
    }
}

