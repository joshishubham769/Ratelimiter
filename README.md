# RateLimiter

A small, thread-safe, in-memory rate limiter for Java. It limits how many requests each user can make within a fixed time window, and it cleans up state for users who have gone idle.

## What it does

You give it a user key (for example a user ID or IP address). It tells you whether that request is allowed or should be rejected.

```java
try (RateLimiter limiter = new RateLimiter(5, 10, 60)) {
    if (limiter.allow("alice")) {
        // handle the request
    } else {
        // reject, e.g. HTTP 429
    }
}
```

In this example, each user can make **5 requests per 10 seconds**, and entries for users idle for more than **60 seconds** are removed.

## Features

- **Per-user limits.** Every user has their own counter and window, so one noisy user doesn't affect others.
- **Fixed-window algorithm.** The first request starts a window. Requests are counted until the limit is reached, and the counter resets once the window expires.
- **Thread-safe without a global lock.** State lives in a `ConcurrentHashMap`, and every read or write of a user's state happens inside `compute` / `computeIfPresent`. Different users proceed in parallel, and the same user is handled atomically.
- **Automatic cleanup of idle users.** A background sweeper thread runs every minute and removes entries that have not been used recently, so memory doesn't grow forever.
- **Safe eviction.** The idle check and the removal happen atomically under the same per-key lock, so a sweep can never delete state that a concurrent request has just refreshed.
- **Daemon sweeper thread.** The cleanup thread is a named daemon thread (`ratelimiter-sweeper`), so it won't keep your JVM alive after `main` finishes.
- **Runtime-adjustable limit.** `countLimit` is `volatile`, so changes made through `setCountLimit` are visible to all threads.
- **Clean shutdown.** Implements `AutoCloseable`; `close()` stops the sweeper thread.

## How it works

1. `allow(user)` calls `states.compute(user, ...)`, which locks only the bucket holding that user.
2. If the user has no state, or their window has expired, a fresh window starts with a count of 0.
3. If the count is below the limit, it is incremented and the request is allowed. Otherwise the request is rejected.
4. `lastAccess` is updated on every request, allowed or rejected.
5. Every minute the sweeper walks the keys and calls `computeIfPresent`, returning `null` (which removes the entry) for users idle longer than `idleNanos`.

## Configuration

| Parameter | Meaning |
|---|---|
| `countLimit` | Maximum requests allowed per window |
| `windowSeconds` | Length of the window in seconds |
| `idleSeconds` | How long a user must be idle before their state is removed |

Keep `idleSeconds >= windowSeconds`. If the idle time is shorter than the window, a throttled user could be evicted early and get their quota reset. Enforcing this in the constructor with `Math.max(idleNanos, windowNanos)` is recommended.

## Design notes and limitations

- **Single JVM only.** State is in memory, so limits are not shared across multiple servers. For a distributed limit, use something like Redis.
- **Fixed windows allow bursts.** A client can make up to 2x the limit around a window boundary (the end of one window plus the start of the next). Use a sliding window or token bucket if that matters.
- **No hard cap on tracked users.** Memory is bounded by idle-time expiry, not by a maximum count. If you need a hard cap, add sampling-based eviction or use a library such as Caffeine.
- **Why not strict LRU?** A strict LRU needs one shared ordering that every request must update, which forces a global lock. This design avoids that on purpose.
- **Keep lambdas short.** Code inside `compute` runs while the bucket lock is held, so avoid slow work or touching the same map from inside it.

## Requirements

- Java 8 or newer
- No external dependencies

## Alternatives

If you don't need to write your own, consider [Caffeine](https://github.com/ben-manes/caffeine) (caching with expiry and size limits) or [Bucket4j](https://github.com/bucket4j/bucket4j) (token-bucket rate limiting).
