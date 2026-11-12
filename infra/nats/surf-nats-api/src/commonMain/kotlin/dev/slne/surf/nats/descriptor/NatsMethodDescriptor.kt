package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.NatsGeneratedApi
import dev.slne.surf.nats.rpc.Recipients
import kotlinx.serialization.KSerializer

/**
 * Describes one rpc of a generated service interface.
 *
 * Only generated code creates instances. The values are resolved from the `.proto` options of the rpc and its
 * service at generation time.
 */
public class NatsMethodDescriptor<Req : Any, Res> @NatsGeneratedApi constructor(
    /**
     * The rpc name as declared in the `.proto`, the last token of the method subject.
     *
     * Renaming the rpc breaks the protocol.
     */
    public val protoName: String,
    /**
     * The name of the suspending interface method that implements the rpc.
     */
    public val kotlinName: String,
    /**
     * Whether calls are stored durably in a stream instead of being sent as core requests.
     */
    public val persistent: Boolean,
    /**
     * The caller-side timeout in seconds, or `null` for the default of 5 seconds for core methods and 60 seconds for
     * persistent methods.
     */
    public val callTimeoutSeconds: Int?,
    /**
     * Whether a call reaches one instance or every instance that serves the service.
     */
    public val recipients: Recipients,
    /**
     * What the interface method returns.
     */
    public val resultKind: NatsResultKind,
    /**
     * The serializer of the request message. A method without parameters uses the serializer of
     * `google.protobuf.Empty`.
     */
    public val requestSerializer: KSerializer<Req>,
    /**
     * The serializer of the response message, `null` exactly when [resultKind] is [NatsResultKind.UNIT].
     */
    public val responseSerializer: KSerializer<*>?,
)
