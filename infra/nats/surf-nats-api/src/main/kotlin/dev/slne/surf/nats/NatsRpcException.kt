@file:Suppress("CanBeParameter")

package dev.slne.surf.nats

import java.io.Serial
import kotlin.time.Duration

open class NatsRpcException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 2052306227954268506L
    }
}

class NatsRemoteException(
    val remoteType: String,
    val remoteMessage: String,
) : NatsRpcException("$remoteType: $remoteMessage") {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 769895995432718802L
    }
}

class NatsNoRespondersException(
    val subject: String,
) : NatsRpcException("No responders for $subject") {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 8318956389694705193L
    }
}

class NatsRequestTimeoutException(
    val subject: String,
    val timeout: Duration,
) : NatsRpcException("No response on $subject within $timeout") {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 6955693591776044694L
    }
}