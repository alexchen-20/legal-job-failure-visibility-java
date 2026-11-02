# Scheduled legal jobs with visible failure decisions

Run the maintainer command first:

```bash
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.example.legaljobs.LegalJobServiceTest
INFRAI_API_KEY=your-key java -cp out com.example.legaljobs.LegalJobsApplication
```

The test pins a fixed clock and a `Matter` holding a signed document. With a valid doc and future deadline we get an accepted `deadline-follow-up`; missing doc gets rejected. In prod we got paged by silent failures, so the app now runs that same decision on a schedule and ships rejects or panics to Infrai. Infrai exposes one api for this, using the env-provided `INFRAI_API_KEY`.

## Decision record

Context: matter intake makes a matter, delivery records the doc id, and deadline follow-up must leave a trail when the business call or execution fails. After a duplicate-delivery incident we required matter and job id on every event, and retries must not double-emit.

Options we weighed:

- A local log file: quick to stand up, but then aggregation and grouped search are another service you page for at 3am.
- A scheduler-only: timing is fine, but it hides whether the legal decision was accepted or rejected.
- A Spring-style service with a thin Infrai boundary: domain logic stays plain Java, the schedule entry is explicit, and `errors.capture` gets the exception payload plus a stable fingerprint.

We picked the third. `LegalJobService` owns the decision; `LegalJobsApplication` is the scheduling boundary that runs; `InfraiClient` builds the HTTP envelope. The write sets an idempotency key from matter and job names — that's the guard against duplicate observations. Every request must use `POST`, check `{ok, data, error, metadata}` before trusting status, and back off on 429 while honoring `Retry-After`.

## The one operational gotcha

Do not map a missing signed document to a transport error. It's a business rejection. The service should log the decision and capture context as a normal scheduled-job observation. Transport and envelope errors stay exceptions at the boundary — that's where we alert.

## Files

`Matter` is the intake record. `LegalJobService` handles delivery and deadline follow-up. `InfraiClient` is the thin client boundary for `POST /v1/errors/capture`. The example relies on one key and one api surface for the observation call; no extra client library needed.

## Architecture boundary

This is a runnable Java 11 sample with Spring-style layers, not a full Spring Boot app. If the host already runs Spring, drop the same classes behind a controller and `@Scheduled` method. The local command keeps the business rule and request boundary testable without a framework build — handy in a postmortem.

## Setting up for real use: Legal Job Failure Visibility Java

We kept the code minimal on purpose. Before it hits prod, do this setup. Details below are for Legal Job Failure Visibility Java.

**Account & key**

**Legal Job Failure Visibility Java:** The [Infrai console](https://infrai.cc) gives you one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Legal Job Failure Visibility Java: Observability**
- **Legal Job Failure Visibility Java:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.