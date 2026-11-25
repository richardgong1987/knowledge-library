# Advanced System Design: Tiny NestJS Interview Lab

**Objective:** solve deep problems with small, inspectable implementations. This is an original educational repository example, not a Meta/Google/Microsoft implementation or a copy of an interview prompt.

- **Framework:** NestJS **12.6.0** (stable checked on 25 November 2026), TypeScript **7.0.2**, Node.js 22.12+ (Node 26 on CI).
- **Core algorithms:** [src/core.ts](src/core.ts), independent of Nest and external databases.
- **HTTP adapter:** [src/main.ts](src/main.ts), one NestJS controller and a provider.
- **Executable tests:** [test/core.test.cjs](test/core.test.cjs), validating 13 behaviours.

## Run locally

From repository root:

~~~bash
cd system-design/interviews/lab
npm install
npm run build
npm test
npm start
~~~

Service: http://127.0.0.1:4300/lab/health. No Docker, database or Redis required.

### Multi-tenant fairness and chunking

~~~bash
curl -s -X POST http://127.0.0.1:4300/lab/files \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"big","fileId":"gigabytes","units":23,"chunkSize":5}'

curl -s -X POST http://127.0.0.1:4300/lab/jobs \
  -H 'Content-Type: application/json' \
  -d '{"tenant":"small","id":"tiny","cost":1}'

curl -s http://127.0.0.1:4300/lab/jobs/next
~~~

Repeat the final request to see **Deficit Round Robin** select bounded-size work. Units represent *estimated compute cost*, **not bytes**. A real pipeline must measure tokenisation, embedding, OCR and parsing effort separately.

### Idempotent operation

~~~bash
curl -s -X POST http://127.0.0.1:4300/lab/idempotency \
  -H 'Content-Type: application/json' \
  -d '{"key":"payment-1","fingerprint":"amount=100"}'
~~~

Repeating the same JSON receives the stored result. Using the same key with another fingerprint signals a conflict.

### Cache versioning

~~~bash
curl -s -X POST http://127.0.0.1:4300/lab/cache \
  -H 'Content-Type: application/json' \
  -d '{"key":"user:7","version":2,"value":"new"}'
curl -s -X POST http://127.0.0.1:4300/lab/cache \
  -H 'Content-Type: application/json' \
  -d '{"key":"user:7","version":1,"value":"old"}'
curl -s 'http://127.0.0.1:4300/lab/cache?key=user:7'
~~~

Older version 1 cannot overwrite already-present version 2.

### Ordering, reservation, hashing and feed

- POST /lab/sequence: JSON key/seq/value; send sequence 2 followed by sequence 1; GET /lab/sequence?key=order:7 returns ordered results.
- POST /lab/seat/hold: JSON seat/owner/nowMs/ttlMs; confirm with POST /lab/seat/confirm, including returned token.
- GET /lab/shard?key=user:7: rendezvous-selected node.
- POST /lab/feed/follow and /lab/feed/post; GET /lab/feed?reader=alice.
- POST /lab/search/record and GET /lab/search/suggest?prefix=app.
- POST /lab/crawl, then GET /lab/crawl/next?nowMs=0&delayMs=1000.
- POST /lab/rate: JSON key and nowMs.
- POST /lab/rank/merge: a JSON list of shard results and required k.

## Guarantees and non-guarantees

The algorithms demonstrate **local properties**: round-robin credits, bounded chunk costs, rate-token accounting, in-flight request coalescing, event ordering by sequence, stale cache-fill rejection, consistent key reassignment, feed selection, top-k merging, token fencing, and per-host pacing.

This implementation is **not safe for production use**. It has no durable state, authentication, distributed locks, multi-node linearizability, replicated idempotency store, broker acknowledgements, real web crawling, real payments, crash recovery, database transactions, or privacy enforcement.

For deployment-scale correctness you need specific external primitives:
- A **transactional store with uniqueness constraints** for idempotent effects and bookings.
- Durable queues, **inbox/outbox** and per-entity sequence enforcement for messaging.
- A coordinated distributed limiter if the rate limit spans multiple app instances.
- A persistent fair queue with an atomic lease/fencing protocol for scheduling.
- Version-aware caching *on every write and refill path*.
- A crawler that respects robots policies, SSRF defences and site-specific constraints.

The study pages state each of these boundaries explicitly and show how to strengthen one idea without adding arbitrary architecture.

## Sources

- [NestJS official documentation](https://docs.nestjs.com/first-steps) and [Nest version compatibility](https://docs.nestjs.com/faq/versions)
- [DRR, Shreedhar and Varghese](https://web.stanford.edu/class/ee384x/EE384X/papers/DRR.pdf)
- [Meta's cache consistency engineering article](https://engineering.fb.com/2022/06/08/core-infra/cache-made-consistent/)
- [Microsoft Azure idempotent consumers](https://learn.microsoft.com/en-us/azure/architecture/patterns/idempotent-consumer)
- [Google Research, consistent hashing with bounded loads](https://research.google/blog/consistent-hashing-with-bounded-loads/)
