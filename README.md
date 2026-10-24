# Outbox Relay Retry Demo

This small demo accompanies the TechShare article [Outbox Relay Retries: At-Least-Once Delivery Without Duplicate Events](https://tec-share.com/2026/10/16/outbox-relay-retries-at-least-once-delivery-without-duplicate-events/).

It focuses on the failure window between a broker publish and the database update that marks an Outbox row as sent. The demo keeps one stable `event_id`, schedules retry with exponential backoff, moves exhausted events to `DEAD`, and records processed event IDs on the consumer side.

## What is included

- `schema.sql` creates the Outbox and consumer idempotency tables.
- `src/main/java/com/tecshare/outbox/OutboxRelay.java` contains the claim, publish, retry, and dead-letter flow.
- `src/test/java/com/tecshare/outbox/OutboxRelayTest.java` exercises a publish failure, a crash after publish, and duplicate consumption.

The Java classes use small interfaces so the failure points can be replaced with a real MySQL repository and broker client without hiding the delivery semantics.

## Run the tests

```bash
mvn test
```

The tests print the event state after every attempt. The important case is `published=true` followed by a simulated crash before `markSent`: the next poll publishes the same event ID again, while the consumer applies the business change only once.

## Production notes

The sample is intentionally small. A production relay should use MySQL 8 `FOR UPDATE SKIP LOCKED`, a lease timeout, bounded exponential backoff with jitter, metrics for event age and dead rows, and a deliberate replay operation for `DEAD` events.

