package dev.slne.surf.nats

import dev.slne.surf.nats.descriptor.NatsMethodDescriptor
import dev.slne.surf.nats.descriptor.NatsParameter
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.descriptor.NatsServiceDescriptor
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.test.fixtures.Tag
import dev.slne.surf.nats.test.proto.v1.Text
import dev.slne.surf.nats.types.EmptySerializer
import kotlin.time.Duration

// Service interfaces with hand-written descriptors. They stand in for contracts of other modules, which one generator
// run never sees together, and for descriptors that do not match their interface.

// Service surf.nats.test.v1.<service> with one `rpc <Name>(google.protobuf.Empty) returns (Text)` per entry of rpcs
fun <S : Any> textService(
    service: String,
    rpcs: List<String> = listOf("ping"),
): NatsServiceDescriptor<S> = NatsServiceDescriptor(
    protoName = "surf.nats.test.v1.$service",
    methods = rpcs.map { rpc ->
        NatsMethodDescriptor<Unit, Text>(
            protoName = rpc.replaceFirstChar(Char::uppercaseChar),
            kotlinName = rpc,
            persistent = false,
            callTimeoutSeconds = null,
            recipients = Recipients.ANY_INSTANCE,
            resultKind = NatsResultKind.VALUE,
            parameters = emptyList(),
            requestSerializer = EmptySerializer,
            responseSerializer = Text.serializer(),
            toRequest = { },
            toArguments = { emptyArray() },
        )
    },
)

interface ClashServiceA {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<ClashServiceA> = textService("NameClashService")
    }
}

interface ClashServiceB {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<ClashServiceB> = textService("NameClashService")
    }
}

interface PingService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<PingService> = textService("PingService")
    }
}

// The descriptor describes an rpc `pong` the interface does not declare
interface MissingMethodService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<MissingMethodService> =
            textService("MissingMethodService", rpcs = listOf("ping", "pong"))
    }
}

// Declares a method `extra` the descriptor does not describe
interface UndescribedMethodService {
    suspend fun ping(): Text

    suspend fun extra(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<UndescribedMethodService> = textService("UndescribedMethodService")
    }
}

// Two rpcs of the descriptor resolve to the same interface method
interface DuplicateRpcService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<DuplicateRpcService> =
            textService("DuplicateRpcService", rpcs = listOf("ping", "ping"))
    }
}

// find(Tag) compiles to the mangled find-<hash>, so the rpc `find` with one parameter matches both overloads
interface OverloadedService {
    suspend fun find(tag: Tag): Text

    suspend fun find(name: String): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<OverloadedService> = NatsServiceDescriptor(
            protoName = "surf.nats.test.v1.OverloadedService",
            methods = listOf(
                NatsMethodDescriptor<Unit, Text>(
                    protoName = "Find",
                    kotlinName = "find",
                    persistent = false,
                    callTimeoutSeconds = null,
                    recipients = Recipients.ANY_INSTANCE,
                    resultKind = NatsResultKind.VALUE,
                    parameters = listOf(NatsParameter("name", String::class, nullable = false)),
                    requestSerializer = EmptySerializer,
                    responseSerializer = Text.serializer(),
                    toRequest = { },
                    toArguments = { emptyArray() },
                ),
            ),
        )
    }
}

// The rpc name "Pi ng" is no proto identifier
interface InvalidRpcNameService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<InvalidRpcNameService> = NatsServiceDescriptor(
            protoName = "surf.nats.test.v1.InvalidRpcNameService",
            methods = textService<InvalidRpcNameService>("InvalidRpcNameService").methods.map { method ->
                NatsMethodDescriptor<Unit, Text>(
                    protoName = "Pi ng",
                    kotlinName = method.kotlinName,
                    persistent = false,
                    callTimeoutSeconds = null,
                    recipients = Recipients.ANY_INSTANCE,
                    resultKind = NatsResultKind.VALUE,
                    parameters = emptyList(),
                    requestSerializer = EmptySerializer,
                    responseSerializer = Text.serializer(),
                    toRequest = { },
                    toArguments = { emptyArray() },
                )
            },
        )
    }
}

// A descriptor without rpcs on an interface without methods
interface EmptyService {
    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<EmptyService> = textService("EmptyService", rpcs = emptyList())
    }
}

// The descriptor types the Duration parameter (JVM long, mangled method name) as the String-backed value class Tag
interface MismatchedValueClassService {
    suspend fun pause(duration: Duration): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<MismatchedValueClassService> = NatsServiceDescriptor(
            protoName = "surf.nats.test.v1.MismatchedValueClassService",
            methods = listOf(
                NatsMethodDescriptor<Unit, Text>(
                    protoName = "Pause",
                    kotlinName = "pause",
                    persistent = false,
                    callTimeoutSeconds = null,
                    recipients = Recipients.ANY_INSTANCE,
                    resultKind = NatsResultKind.VALUE,
                    parameters = listOf(NatsParameter("duration", Tag::class, nullable = false)),
                    requestSerializer = EmptySerializer,
                    responseSerializer = Text.serializer(),
                    toRequest = { },
                    toArguments = { emptyArray() },
                ),
            ),
        )
    }
}
