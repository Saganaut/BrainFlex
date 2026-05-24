<!--
  Runbook: how to use the BrainFlex observability foundation day-to-day —
  emitting correlated logs front and back, running prod-shaped JSON logging
  locally, reading the X-Request-Id ↔ traceId thread, and exercising the
  LocalStack CloudWatch path. The "why" lives in the ADR
  (z-docs/decisions/001-observability-stack.md); this file is the "how".
-->

# Using the observability stack

The architecture and the reasoning behind it are in **[ADR 001 — Observability & logging stack](../decisions/001-observability-stack.md)**. This runbook is the operational how-to: what to call, how to run it, and how to confirm a log line on the backend matches a request from the browser.

The one idea to hold onto: **every request carries an `X-Request-Id`**. The frontend mints it, the backend adopts it into the SLF4J MDC as `traceId`, and it shows up in log lines, error response bodies, and the `X-Request-Id` response header. That single id is how you tie a user click to a server-side stack trace.

---

## Backend: emitting logs

Keep the existing convention — a hand-rolled logger per class (we do **not** use `@Slf4j`):

```java
private static final Logger log = LoggerFactory.getLogger(MyService.class);
...
log.info("Deck published deckId={} elements={}", deckId, count);
```

You never set `traceId` or `userId` yourself — `MdcLoggingFilter` (`backend/.../web/MdcLoggingFilter.java`) has already put them in the MDC for the request, and `logback-spring.xml` emits them on every record. Just log a clear message with structured key=value context.

**Do not catch-and-swallow.** Let exceptions propagate to `GlobalExceptionHandler` (the single `@RestControllerAdvice`), which logs 5xx with the full stack trace + `traceId` and returns an RFC 9457 `ProblemDetail`. See [EXCEPTION-RULES](../rules/EXCEPTION-RULES.md).

### Run with dev (readable) logging

Default boot prints the familiar coloured console line, with `[traceId]` inserted:

```bash
set -a && . ./dev.env && set +a   # see note below — required for placeholder resolution
cd backend && ./mvnw spring-boot:run
```

> The `set -a … dev.env … set +a` step is mandatory for a non-interactive boot. `application.properties` binds `logging.level.org.springframework.security=${LOGGING_LEVEL}` very early — before `DotenvEnvironmentPostProcessor` adds its property source — so the placeholders must already be real OS env vars or startup fails with `Value: "${LOGGING_LEVEL}"`.

### Run with prod (JSON) logging locally

To see exactly what CloudWatch will ingest — one-line JSON on stdout with `traceId`/`userId` fields:

```bash
set -a && . ./dev.env && set +a
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

In production nothing changes in the app: stdout is shipped to CloudWatch Logs by the container log driver (awslogs on ECS / the CloudWatch agent on EC2). The app makes **no** AWS calls to log.

---

## Frontend: emitting logs

Import the shared logger — never call `console.*` directly in new code:

```ts
import { logger } from "@/utils/logger";

logger.info("Deck saved", { deckId });
logger.error("Failed to load deck", { deckId, err });
```

- **Dev**: pretty, level-prefixed console output.
- **Prod**: quiet for `debug`/`info`/`warn`; `error` still hits `console.error`. The single Sentry seam is the `PROD SEAM` comment in `frontend/src/utils/logger.ts` — that one file is all that changes when Sentry lands.

You get error reporting for free in two places, so you rarely log errors by hand:

- **API failures** — `emptyApi.ts`'s `baseQueryWithAuthPrompt` logs every non-401 failure with the server `traceId` (read off the echoed `X-Request-Id` response header). 401s are an expected auth-prompt signal and are intentionally not logged as errors.
- **React render crashes** — the root `ErrorBoundary` (in `routes/__root.tsx`) logs and renders `ServerErrorPage` instead of a white screen. `window.onerror` / `unhandledrejection` are wired in `main.tsx`.

You normally only call `logger.*` directly for domain events and caught-and-handled conditions.

---

## Verifying the correlation thread end-to-end

1. **Server mints an id when none is sent**, and echoes it:

   ```bash
   curl -i http://localhost:8080/actuator/health | grep -i x-request-id
   # → X-Request-Id: 1b9f...   (a fresh UUID)
   ```

2. **Server honours a supplied id** — and the same id appears in the log line:

   ```bash
   curl -s -H "X-Request-Id: smoke-123" \
     http://localhost:8080/api/decks/000000000000000000000000
   # → {"detail":"Deck not found", ..., "traceId":"smoke-123"}
   ```

   In the backend console you'll see the matching line, e.g.
   `... [smoke-123] c.b...DeckController : ...`.

3. **From the browser**: open DevTools → Network, trigger any API call, and confirm the request's `X-Request-Id` header equals the response's `X-Request-Id`. Grep the backend logs for that id to find the server side of the same request.

---

## Health & metrics (Actuator)

Exposed endpoints are limited to `health,info,metrics` (`application.properties`), with health probes enabled:

```bash
curl -s http://localhost:8080/actuator/health      # {"status":"UP", ...}
curl -s http://localhost:8080/actuator/metrics      # list of metric names
curl -s http://localhost:8080/actuator/metrics/jvm.memory.used
```

The app-specific `/api/health` controller is separate and is what the frontend may probe.

---

## LocalStack (CloudWatch parity)

LocalStack runs alongside Garage (Garage stays our S3 — LocalStack is **only** `cloudwatch,logs`):

```bash
docker compose up -d localstack        # edge port :4566
export AWS_ENDPOINT_URL=http://localhost:4566 AWS_REGION=us-east-1   # already in dev.env
awslocal cloudwatch list-metrics       # or: aws --endpoint-url=$AWS_ENDPOINT_URL cloudwatch list-metrics
```

Today this mainly pre-positions the future metrics path: once `micrometer-registry-cloudwatch2` is wired (deferred), published custom metrics can be inspected here without touching real AWS. For **logs**, the prod pattern is stdout → log driver, so locally the prod-profile JSON on stdout (above) is the equivalent — there is no app-level CloudWatch log appender to emulate.

---

## Deferred — where the vendor seams are

These are intentionally **not** wired yet (see the ADR's deferred section). When you pick them up:

| To add | Edit |
| --- | --- |
| Sentry error tracking + Web Vitals + session replay (frontend) | the `PROD SEAM` in `frontend/src/utils/logger.ts`; add `@sentry/react` |
| Sentry (backend) | `backend/pom.xml` (commented intent next to the logstash encoder) + init in config |
| CloudWatch custom metrics | `backend/pom.xml` → `micrometer-registry-cloudwatch2`; validate against LocalStack |
| X-Ray / OpenTelemetry tracing | propagate into the same `traceId` MDC key |
