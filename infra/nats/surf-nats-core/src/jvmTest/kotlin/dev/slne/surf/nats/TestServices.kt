package dev.slne.surf.nats

import dev.slne.surf.nats.descriptor.NatsMethodDescriptor
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.descriptor.NatsServiceDescriptor
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.test.proto.v1.Text
import dev.slne.surf.nats.types.EmptySerializer
import kotlinx.serialization.KSerializer

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
            requestSerializer = EmptySerializer,
            responseSerializer = Text.serializer(),
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

// The rpc `find` with a request matches both overloads
interface OverloadedService {
    suspend fun find(request: Text): Text

    suspend fun find(request: String): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<OverloadedService> = NatsServiceDescriptor(
            protoName = "surf.nats.test.v1.OverloadedService",
            methods = listOf(
                NatsMethodDescriptor<Text, Text>(
                    protoName = "Find",
                    kotlinName = "find",
                    persistent = false,
                    callTimeoutSeconds = null,
                    recipients = Recipients.ANY_INSTANCE,
                    resultKind = NatsResultKind.VALUE,
                    requestSerializer = Text.serializer(),
                    responseSerializer = Text.serializer(),
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
                    requestSerializer = EmptySerializer,
                    responseSerializer = Text.serializer(),
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

// Each method differs from the request shape its rpc describes: pause takes a request for an Empty rpc, resume takes
// none for a Text rpc and count takes a primitive
interface MismatchedRequestService {
    suspend fun pause(request: Text): Text

    suspend fun resume(): Text

    suspend fun count(request: Int): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<MismatchedRequestService> = NatsServiceDescriptor(
            protoName = "surf.nats.test.v1.MismatchedRequestService",
            methods = listOf(
                textMethod("Pause", "pause", EmptySerializer),
                textMethod("Resume", "resume", Text.serializer()),
                textMethod("Count", "count", Text.serializer()),
            ),
        )
    }
}

// The test config.yml (TestNatsInstance) limits it to one concurrent handler
interface LimitedService {
    suspend fun hold(): Text

    suspend fun early(): Text

    suspend fun late(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<LimitedService> =
            textService("LimitedService", listOf("hold", "early", "late"))
    }
}

private fun <Req : Any> textMethod(protoName: String, kotlinName: String, request: KSerializer<Req>) =
    NatsMethodDescriptor<Req, Text>(
        protoName = protoName,
        kotlinName = kotlinName,
        persistent = false,
        callTimeoutSeconds = null,
        recipients = Recipients.ANY_INSTANCE,
        resultKind = NatsResultKind.VALUE,
        requestSerializer = request,
        responseSerializer = Text.serializer(),
    )
