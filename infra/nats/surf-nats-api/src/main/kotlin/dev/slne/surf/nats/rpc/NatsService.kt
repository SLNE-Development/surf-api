package dev.slne.surf.nats.rpc

/**
 * Marks an interface as a remotely callable service.
 *
 * Remote methods must be suspending and have unique Kotlin names. Overloads, extension receivers,
 * and context parameters are not supported. Default interface methods cannot be called remotely.
 * Parameter and return types must be serializable with the client's serializers module.
 * Value-class parameters are supported. Nullable `Unit?`, nullable `Answer<T>?`, and `Answer<*>`
 * return types are rejected.
 *
 * Delivery depends on [NatsPersistent], [NatsRecipients], and the return type:
 *
 * | Persistence | Recipients | Return type | Call semantics |
 * | --- | --- | --- | --- |
 * | Non-persistent | [Recipients.ANY_INSTANCE] | Value or [Answer] | Waits for one instance's response. Remote failures become [NatsRemoteException]. |
 * | Non-persistent | [Recipients.ANY_INSTANCE] | [Unit] | Waits for handler completion. Remote failures propagate. |
 * | Non-persistent | [Recipients.ALL_INSTANCES] | [Unit] | Publishes to all instances and returns immediately, without handler error feedback. |
 * | Non-persistent | [Recipients.ALL_INSTANCES] | [Answer] | Returns the first successfully decoded [Answer.Known]. If no instance answers [Answer.Known] and an instance failed, that failure is thrown. Otherwise, returns [Answer.Unknown]. |
 * | Persistent | [Recipients.ANY_INSTANCE] | [Unit] | Returns after durable storage is confirmed. Processing may occur later. |
 * | Persistent | [Recipients.ANY_INSTANCE] | Value or [Answer] | Stores the request durably and waits for a response. |
 *
 * Methods using [Recipients.ALL_INSTANCES] cannot return plain values or be persistent.
 * Changing a method's return type can change its delivery semantics.
 *
 * Non-persistent fan-out ([Recipients.ALL_INSTANCES] with [Unit]) drops requests if the caller's
 * deadline expires before handling starts. Once started, handlers have no call timeout and run
 * until they complete or their registration is cancelled.
 *
 * For [Recipients.ALL_INSTANCES] with [Answer], the first [Answer.Known] ends the call immediately.
 * If no known answer arrives, remote failures or response decoding failures are thrown. If no such
 * failure occurs, the call returns [Answer.Unknown] when all known instances have answered, no
 * responders exist, or the timeout expires. These calls never throw [NatsCallTimeoutException].
 * Early completion requires presence information. For about one second after the first proxy is
 * created and after each reconnect, a call that receives no [Answer.Known] may wait until its timeout.
 *
 * Persistent [Unit] calls target one instance per delivery and can be stored while no instance is
 * online. Handler failures are retried with backoff, with at most five deliveries. After the last
 * failed delivery, the message is moved to a dead-letter stream. Handlers exceeding ten minutes
 * are cooperatively cancelled and count as failed deliveries. Persistent calls returning
 * a value or [Answer] report handler failures as [NatsRemoteException] without retrying them.
 * Persistent handlers must be idempotent because requests can be delivered more than once.
 *
 * Default call timeouts are five seconds for non-persistent methods and 60 seconds for persistent
 * methods. See [NatsCallTimeout] and [withNatsCallTimeout]. Hosts must have synchronized clocks because
 * request deadlines are absolute.
 *
 * Non-persistent [Recipients.ANY_INSTANCE] calls throw [NatsNoRespondersException] immediately
 * when no instance serves the method. [Recipients.ANY_INSTANCE] calls throw [NatsCallTimeoutException]
 * when no response or storage confirmation arrives within the timeout. Remote failures are reported
 * as [NatsRemoteException] according to the delivery semantics above.
 *
 * Expected domain outcomes belong in return types, such as sealed results or [Answer]. Exceptions
 * indicate bugs. [NatsRemoteException] carries only the remote failure's type name and message.
 *
 * Service, method, and parameter names are part of the protocol. Derived names change when their
 * Kotlin declarations are renamed. For callers using a different codebase or version, set all names
 * explicitly using [name], [NatsMethod], and [NatsParam].
 *
 * ```kotlin
 * @NatsService("profiles")
 * interface Profiles {
 *     @NatsRecipients(Recipients.ANY_INSTANCE)
 *     suspend fun displayName(id: String): String
 *
 *     @NatsPersistent
 *     suspend fun recordVisit(id: String)
 *
 *     @NatsRecipients(Recipients.ALL_INSTANCES)
 *     suspend fun find(id: String): Answer<String>
 * }
 *
 * suspend fun example(client: NatsClient) {
 *     client.registerService<Profiles>(object : Profiles {
 *         override suspend fun displayName(id: String) = id
 *         override suspend fun recordVisit(id: String) { println(id) }
 *         override suspend fun find(id: String): Answer<String> = Answer.Unknown
 *     })
 *     val profiles = client.service<Profiles>()
 *     profiles.recordVisit("alice")
 * }
 * ```
 *
 * @property name the kebab-case service name, or the interface's simple name converted to kebab-case
 * when empty.
 * `dlq` and names ending in `-dlq` are reserved.
 * @property global whether any client may serve or call this service. When false, callers and
 * implementations must use the same client name. When true, implementations across clients share
 * one responder group. The service name must be unique across applications.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class NatsService(
    val name: String = "",

    val global: Boolean = false,
)
