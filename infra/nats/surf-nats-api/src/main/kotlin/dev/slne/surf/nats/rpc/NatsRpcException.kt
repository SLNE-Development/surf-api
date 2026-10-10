@file:Suppress("CanBeParameter")

package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import java.io.Serial
import kotlin.time.Duration

public open class NatsRpcException @InternalNatsApi constructor(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = 2052306227954268506L
    }
}

public class NatsRemoteException @InternalNatsApi constructor(
    public val remoteType: String,
    public val remoteMessage: String,
) : NatsRpcException("$remoteType: $remoteMessage") {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = 769895995432718802L
    }
}

public class NatsNoRespondersException @InternalNatsApi constructor(
    public val subject: String,
) : NatsRpcException("No responders for $subject") {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = 8318956389694705193L
    }
}

public class NatsRequestTimeoutException @InternalNatsApi constructor(
    public val subject: String,
    public val timeout: Duration,
) : NatsRpcException("No response on $subject within $timeout") {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = 6955693591776044694L
    }
}