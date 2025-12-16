Below is a **senior–principal-architect–level prompt** you can use verbatim to drive a **production-grade Spring Boot + Redis design and implementation**.
It is intentionally **precise, opinionated, and review-ready**, and it **explicitly highlights Redis caching capabilities and configuration choices**.

---

## 📌 PRODUCTION-GRADE ARCHITECT PROMPT

**Spring Boot Application with Redis Caching (Enterprise-Ready)**

---

### 🎯 ROLE & EXPECTATION

> Act as a **Senior Principal Architect** designing a **production-ready Spring Boot 3.x application** that uses **Redis as a distributed cache**.
>
> The system must meet **enterprise non-functional requirements**: performance, resilience, security, observability, scalability, and operational simplicity.
>
> This is **not a tutorial**. All decisions must be justifiable in an architecture review.

---

## 🏗 SYSTEM CONTEXT

> The application exposes REST APIs that:
>
> * Serve **read-heavy business data**
> * Call downstream systems (DB / ESB / external APIs)
> * Require **aggressive caching to offload backend systems**
>
> Redis is used **exclusively as a cache**, not as a system of record.

---

## 🧠 REDIS CACHING OBJECTIVES (VERY IMPORTANT)

> Design Redis usage to:
>
> * Reduce backend latency
> * Absorb traffic spikes
> * Protect downstream systems from overload
> * Provide predictable eviction behavior
> * Fail gracefully without data corruption

Redis cache must be:

* **Loss-tolerant**
* **Eviction-friendly**
* **Horizontally scalable**
* **Operationally observable**

---

## 🔴 REDIS ARCHITECTURE REQUIREMENTS

### Redis Topology

* Redis Cluster (not standalone)
* Minimum:

  * 2 masters
  * 2 replicas
* Deployed in private network
* TLS enabled

### Memory & Eviction

* `maxmemory` configured explicitly
* Eviction policy:

  ```
  allkeys-lru
  ```
* Cache must never block writes due to memory exhaustion

### Persistence

* **Disabled or minimal**

  * RDB optional
  * AOF disabled
* Cache must be disposable and self-healing

### Availability

* Replica promotion on master failure
* No cross-site replication required
* Cache warm-up strategy on restart

---

## ⚙️ SPRING BOOT CACHE DESIGN

### Frameworks & Versions

* Spring Boot 3.x
* Spring Cache Abstraction
* Spring Data Redis (Lettuce client)

---

### Caching Strategy

> Implement **multi-layer caching** with:

* Method-level caching
* Explicit cache names per domain
* Domain-specific TTLs

Example cache categories:

* Reference data (longer TTL)
* User/session data (short TTL)
* Backend response cache (very short TTL)

---

### Cache Patterns to Implement

1. **Read-Through Cache**

   * Cache populated on first read
   * Backend hit only on cache miss

2. **Write-Through / Evict**

   * Writes invalidate or update cache
   * No stale data tolerance

3. **TTL-Driven Expiry**

   * TTL set per cache
   * No eternal keys

4. **Graceful Degradation**

   * Application must work if Redis is unavailable
   * Fallback to direct backend calls

---

## 🔑 REDIS KEY DESIGN

> Keys must be:

* Predictable
* Namespaced
* Versioned

Example:

```
app:v1:user:{userId}
app:v1:product:{productId}
app:v1:refdata:{type}
```

No unbounded key growth.

---

## 🛠 PRODUCTION-GRADE REDIS CONFIGURATION

### Redis Client

* Lettuce (non-blocking)
* Connection pooling enabled
* Timeouts configured

Example expectations:

* Command timeout
* Connection timeout
* Circuit breaking behavior

---

### Spring Boot Configuration

> Provide:

* `application.yml` snippets
* Separate profiles:

  * local
  * staging
  * production

Include:

* Cache TTL per cache name
* Redis cluster configuration
* Serialization strategy (JSON / Kryo / ProtoBuf)

---

## 🔒 SECURITY CONSIDERATIONS

* Redis authentication enabled
* ACLs where supported
* No sensitive data cached
* TLS between app and Redis

---

## 📈 OBSERVABILITY & OPERATIONS

> The system must expose:

* Cache hit/miss metrics
* Redis latency metrics
* Eviction count
* Connection pool usage

Integrate with:

* Micrometer
* Prometheus
* Centralized logging

---

## 🚨 FAILURE & EDGE CASE HANDLING

> Explicitly address:

* Redis outage
* Partial cluster failure
* Cache stampede prevention
* Cold start behavior
* Cache inconsistency risks

---

## 📦 DELIVERABLES

> Generate:

1. High-level architecture (textual)
2. Redis cache design decisions
3. Spring Cache configuration
4. Redis config (production-grade)
5. Code snippets (annotations + config)
6. Operational trade-offs
7. Common pitfalls and how this design avoids them

---

## ⚠️ CONSTRAINTS

* Do NOT treat Redis as a database
* Do NOT use default configurations
* Do NOT assume infinite memory
* Design must survive production traffic spikes

---

## 🧠 FINAL QUALITY BAR

> The output must be something:

* A senior engineer could implement directly
* A principal architect could defend
* An SRE would be comfortable operating


