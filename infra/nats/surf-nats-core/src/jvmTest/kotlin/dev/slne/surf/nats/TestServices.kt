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

// One `rpc <Name>(google.protobuf.Empty) returns (Text)` per entry of rpcs
fun <S : Any> textService(
    name: String,
    global: Boolean = false,
    majorVersion: Int = 1,
    rpcs: List<String> = listOf("ping"),
): NatsServiceDescriptor<S> = NatsServiceDescriptor(
    protoName = "surf.nats.test.v$majorVersion.${name.replace("-", "")}",
    name = name,
    global = global,
    majorVersion = majorVersion,
    methods = rpcs.map { rpc ->
        NatsMethodDescriptor<Unit, Text>(
            protoName = rpc.replaceFirstChar(Char::uppercaseChar),
            name = rpc,
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
        val NATS_DESCRIPTOR: NatsServiceDescriptor<ClashServiceA> = textService("name-clash")
    }
}

interface ClashServiceB {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<ClashServiceB> = textService("name-clash")
    }
}

interface GlobalClashService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<GlobalClashService> =
            textService("name-clash-mixed", global = true)
    }
}

interface PrefixedClashService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<PrefixedClashService> = textService("name-clash-mixed")
    }
}

// The descriptor describes an rpc `pong` the interface does not declare
interface MissingMethodService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<MissingMethodService> =
            textService("missing-method", rpcs = listOf("ping", "pong"))
    }
}

// Declares a method `extra` the descriptor does not describe
interface UndescribedMethodService {
    suspend fun ping(): Text

    suspend fun extra(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<UndescribedMethodService> = textService("undescribed-method")
    }
}

// Two rpcs of the descriptor resolve to the same interface method
interface DuplicateRpcService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<DuplicateRpcService> =
            textService("duplicate-rpc", rpcs = listOf("ping", "ping"))
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
            name = "overloaded",
            global = false,
            majorVersion = 1,
            methods = listOf(
                NatsMethodDescriptor<Unit, Text>(
                    protoName = "Find",
                    name = "find",
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

// The method name token "pi ng" makes the composed method subject invalid
interface InvalidSubjectService {
    suspend fun ping(): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<InvalidSubjectService> = NatsServiceDescriptor(
            protoName = "surf.nats.test.v1.InvalidSubjectService",
            name = "invalid-subject",
            global = false,
            majorVersion = 1,
            methods = textService<InvalidSubjectService>("invalid-subject").methods.map { method ->
                NatsMethodDescriptor<Unit, Text>(
                    protoName = method.protoName,
                    name = "pi ng",
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
        val NATS_DESCRIPTOR: NatsServiceDescriptor<EmptyService> = textService("empty", rpcs = emptyList())
    }
}

// The descriptor types the Duration parameter (JVM long, mangled method name) as the String-backed value class Tag
interface MismatchedValueClassService {
    suspend fun pause(duration: Duration): Text

    companion object {
        @JvmField
        val NATS_DESCRIPTOR: NatsServiceDescriptor<MismatchedValueClassService> = NatsServiceDescriptor(
            protoName = "surf.nats.test.v1.MismatchedValueClassService",
            name = "mismatched-value-class",
            global = false,
            majorVersion = 1,
            methods = listOf(
                NatsMethodDescriptor<Unit, Text>(
                    protoName = "Pause",
                    name = "pause",
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
