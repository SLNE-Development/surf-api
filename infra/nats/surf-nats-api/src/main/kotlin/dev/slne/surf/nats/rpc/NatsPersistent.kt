package dev.slne.surf.nats.rpc

/**
 * Selects JetStream persistence for service calls.
 *
 * Applies to service interfaces and methods. A method's annotation takes precedence over its
 * interface's annotation. Methods are non-persistent by default. See [NatsService] for delivery
 * semantics for each combination of recipients and return type.
 *
 * Persistent methods cannot use [Recipients.ALL_INSTANCES], and their call timeout must not exceed
 * the two-minute deduplication window. Handlers must be idempotent because messages can be delivered
 * more than once. [NatsPersistentContext] provides the message identity and delivery count.
 *
 * @property enabled whether persistence is enabled. Set to false on a method to override
 * interface-level persistence.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsPersistent(val enabled: Boolean = true)
