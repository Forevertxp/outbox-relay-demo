package com.tecshare.outbox;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OutboxRelayTest {
    @Test
    void retriesThenMarksSent() {
        AtomicInteger calls = new AtomicInteger();
        OutboxRelay relay = new OutboxRelay(event -> {
            if (calls.incrementAndGet() == 1) throw new IllegalStateException("broker timeout");
        });
        Instant start = Instant.parse("2026-10-20T00:00:00Z");
        relay.add(new OutboxRelay.Event("evt-1", 0, OutboxRelay.Status.PENDING, start, null));

        var afterFailure = relay.poll(start);
        assertEquals(OutboxRelay.Status.PENDING, afterFailure.getFirst().status());
        assertEquals(1, afterFailure.getFirst().attempts());

        var afterRetry = relay.poll(afterFailure.getFirst().availableAt());
        assertEquals(OutboxRelay.Status.SENT, afterRetry.getFirst().status());
        assertEquals(2, calls.get());
    }

    @Test
    void duplicateEventIsAppliedOnce() {
        Set<String> processed = new HashSet<>();
        OutboxRelay.Consumer consumer = processed::add;
        assertTrue(consumer.applyOnce("evt-2"));
        assertFalse(consumer.applyOnce("evt-2"));
        assertEquals(Set.of("evt-2"), processed);
    }

    @Test
    void permanentFailureBecomesDead() {
        OutboxRelay relay = new OutboxRelay(event -> { throw new IllegalArgumentException("invalid payload"); });
        Instant start = Instant.parse("2026-10-20T00:00:00Z");
        relay.add(new OutboxRelay.Event("evt-3", 2, OutboxRelay.Status.PENDING, start, null));
        var result = relay.poll(start);
        assertEquals(OutboxRelay.Status.DEAD, result.getFirst().status());
        assertEquals("invalid payload", result.getFirst().lastError());
    }
}

