package dev.slne.surf.redis.util

/**
 * Marks an API as internal to surf-redis.
 *
 * APIs annotated with [InternalRedisAPI] are not part of the public, stable API
 * and may change or be removed at any time without notice.
 *
 * They are intended for internal use by surf-redis components only.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is internal to surf-redis, is not stable, and must not be used by consumers."
)
annotation class InternalRedisAPI
