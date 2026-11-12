package dev.slne.surf.nats.rpc

/**
 * Selects whether a message reaches one or every listening instance of a group.
 *
 * For event listeners ([dev.slne.surf.nats.event.NatsListener.recipients]), the group is every instance of the
 * clients with the same name. For service methods ([dev.slne.surf.nats.descriptor.NatsMethodDescriptor.recipients]),
 * the group is every registration of the proto service, whatever the client name.
 *
 * ## Delivery semantics of service methods
 *
 * The `.proto` contract sets `recipients`, `persistent` and `call_timeout_seconds` in the `(surf.nats.v1.service)`
 * and `(surf.nats.v1.method)` options. A method option overrides the service option. Without options, a method is
 * not persistent and addresses [ANY_INSTANCE]. The response type and [Recipients] determine the Kotlin return type:
 * `google.protobuf.Empty` becomes [Unit], a message becomes that message, and a message of an [ALL_INSTANCES]
 * method becomes [Answer] of that message.
 *
 * The delivery semantics follow from these options and the return type. Changing either changes the semantics for
 * every caller:
 *
 * - [ANY_INSTANCE] with a value: one instance handles the request and the caller suspends until its response
 *   arrives. A failure of the handler is thrown as [NatsRemoteException]. Throws [NatsNoRespondersException] when no
 *   instance serves the method and [NatsCallTimeoutException] when no response arrives in time.
 * - [ANY_INSTANCE] with [Unit]: as above, and the caller suspends until the handler has completed.
 * - [ALL_INSTANCES] with [Unit]: fan-out. Every instance receives the request and the call returns as soon as the
 *   request is handed to the connection, without error feedback. Instances that receive the request after the call
 *   timeout drop it. Handlers that have started run to completion without a time limit.
 * - [ALL_INSTANCES] with [Answer]: every instance is asked and the first [Answer.Known] is returned at once. The call
 *   returns [Answer.Unknown] when every instance answered [Answer.Unknown], no instance serves the method, or the
 *   timeout passes. A failure of one instance is thrown, as [NatsRemoteException] or as the decoding error, only when
 *   no instance answered [Answer.Known]. Never throws [NatsCallTimeoutException]. Without an [Answer.Known], the call
 *   ends before the timeout only when no instance serves the method or a reply shows that every live instance has
 *   answered. During the first second after the first proxy of the service is created on a client, and during the
 *   first second after the first such call following a reconnect, the call does not end early because every
 *   instance has answered.
 * - Persistent with [Unit] (fire): the request is stored durably and the call returns once the server has confirmed
 *   the storage. One instance handles it, also when no instance is running at call time. A failed delivery is
 *   repeated with backoff, up to five deliveries, then moved to the dead-letter stream of the service. Requests that
 *   cannot be decoded or whose payload is gone are dead-lettered at once. A handler running longer than ten minutes
 *   is cancelled and counts as failed. The call timeout covers only the storage confirmation and must not exceed the
 *   two-minute duplicate window.
 * - Persistent with a value (persistent RPC): the request is stored durably, one instance handles it and the caller
 *   suspends until the response arrives. A failure of the handler is thrown as [NatsRemoteException] and is not
 *   retried, except for [NatsRedeliveryException]. The call timeout has no upper bound. Only the phase that waits for
 *   the storage confirmation is limited to two minutes.
 *
 * Persistent methods cannot address [ALL_INSTANCES]. Handlers of persistent methods can read
 * [NatsPersistentContext] and must tolerate repeated delivery of the same message.
 *
 * Every call except a fire carries an absolute deadline taken from the caller's clock, and responders drop requests
 * whose deadline has passed, so the hosts need synchronized clocks. A request or response that exceeds the server's
 * payload limit is moved through the object store, which keeps it for ten minutes, or seven days for a fire request.
 * A call with an offloaded request and a timeout above ten minutes throws [IllegalArgumentException], and an
 * offloaded response in that case reaches the caller as [NatsRemoteException].
 *
 * Expected domain outcomes belong in the response message, for example a status field or an [Answer]. Exceptions
 * reach the caller only as [NatsRemoteException] with the remote type name and message, so reserve them for bugs and
 * infrastructure failures.
 *
 * The following contract covers three of these cases:
 *
 * ```proto
 * service FinderService {
 *   option (surf.nats.v1.service) = { recipients: NATS_RECIPIENTS_ALL_INSTANCES };
 *
 *   rpc Find(FindRequest) returns (FindReply);                 // Answer<FindReply>
 *   rpc Broadcast(BroadcastRequest) returns (google.protobuf.Empty); // fan-out
 *   rpc Lookup(FindRequest) returns (FindReply) {              // FindReply from one instance
 *     option (surf.nats.v1.method) = { recipients: NATS_RECIPIENTS_ANY_INSTANCE };
 *   }
 * }
 * ```
 *
 * The generated interface and a caller:
 *
 * ```kotlin
 * class Finder(private val values: Map<String, String>) : FinderService {
 *     override suspend fun find(request: FindRequest): Answer<FindReply> =
 *         values[request.key]?.let { FindReply(it) }.toAnswer()
 *
 *     override suspend fun broadcast(request: BroadcastRequest) {
 *         println(request.message)
 *     }
 *
 *     override suspend fun lookup(request: FindRequest): FindReply = FindReply(values[request.key])
 * }
 *
 * suspend fun findValue(client: NatsClient, key: String): String? {
 *     val finder = client.service<FinderService>()
 *     return finder.find(FindRequest(key)).getOrNull()?.value
 * }
 * ```
 */
public enum class Recipients {
    /**
     * Addresses one listening instance of the group.
     */
    ANY_INSTANCE,

    /**
     * Addresses every listening instance of the group.
     */
    ALL_INSTANCES
}
