package dev.slne.surf.nats.js

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.external.protobuf.AnyDesc
import dev.slne.surf.nats.js.external.protobuf.DescExtension
import dev.slne.surf.nats.js.external.protobuf.DescField
import dev.slne.surf.nats.js.external.protobuf.DescFile
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.DescService
import dev.slne.surf.nats.js.external.protobuf.getOption
import dev.slne.surf.nats.js.external.protobuf.hasOption
import dev.slne.surf.nats.js.external.protobuf.reflect.ReflectMessage
import dev.slne.surf.nats.protocol.EventOptions
import dev.slne.surf.nats.protocol.MethodOptions
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ProtocolSubjects
import dev.slne.surf.nats.protocol.RecipientsOption
import dev.slne.surf.nats.protocol.ResolvedMethod
import dev.slne.surf.nats.protocol.ResolvedService
import dev.slne.surf.nats.protocol.ResultKindOption
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.protocol.ServiceOptions
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.runtime.rpc.CallFlow
import js.collections.WeakMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class EventContract(
    val typeName: String,
    val subject: String,
    val global: Boolean,
    val majorVersion: Int,
) {
    private val subjects = PerOwner(global, typeName) { owner -> ProtocolSubjects.event(owner, subject, majorVersion) }

    fun subject(owner: String?): String = subjects[owner]
}

internal class ServiceContract(
    val typeName: String,
    val resolved: ResolvedService,
    val majorVersion: Int,
) {
    val name: String get() = resolved.name
    val global: Boolean get() = resolved.global

    private val names = PerOwner(global, typeName) { owner -> ServiceNames(owner, name, majorVersion) }

    fun names(owner: String?): ServiceNames = names[owner]
}

internal class MethodContract(
    val service: ServiceContract,
    val protoName: String,
    resolved: ResolvedMethod,
    val input: DescMessage,
    val output: DescMessage,
) {
    val name: String = resolved.name
    val persistent: Boolean = resolved.persistent
    val callTimeoutSeconds: Int? = resolved.callTimeoutSeconds
    val recipients: Recipients = when (resolved.recipients) {
        RecipientsOption.ALL_INSTANCES -> Recipients.ALL_INSTANCES
        else -> Recipients.ANY_INSTANCE
    }
    val resultKind: NatsResultKind = when (resolved.resultKind) {
        ResultKindOption.UNIT -> NatsResultKind.UNIT
        ResultKindOption.VALUE -> NatsResultKind.VALUE
        ResultKindOption.ANSWER -> NatsResultKind.ANSWER
    }
    val flow: CallFlow = CallFlow.of(persistent, resultKind)
    val label: String = "${service.typeName}.$protoName"
    val defaultTimeout: Duration = resolved.effectiveCallTimeoutSeconds.seconds
    val decoder: ReplyDecoder = ReplyDecoder(output, resultKind)

    private val subjects = PerOwner(service.global, service.typeName) { owner ->
        service.names(owner).methodSubject(name, persistent)
    }

    fun subject(owner: String?): String = subjects[owner]
}

/**
 * Reads the `surf.nats.v1` options of protobuf-es descriptors and resolves them with the shared protocol rules.
 *
 * The options extensions are looked up in the direct imports of the declaring file, so a file that uses surf
 * options must import `surf/nats/v1/options.proto` directly.
 */
internal object Contracts {
    private val events = WeakMap<DescMessage, EventContract>()
    private val methods = WeakMap<DescMethod, MethodContract>()
    private val services = WeakMap<DescService, ServiceContract>()

    fun event(schema: DescMessage): EventContract {
        events.get(schema)?.let { return it }
        return resolveEvent(schema).also { events.set(schema, it) }
    }

    fun method(method: DescMethod): MethodContract {
        methods.get(method)?.let { return it }
        return resolveMethod(method).also { methods.set(method, it) }
    }

    fun isNullable(field: DescField): Boolean {
        val extension = optionExtension(field.parent.file, ProtocolOptions.NULLABLE) ?: return false
        return hasOption(field, extension) && getOption(field, extension) == true
    }

    private fun resolveEvent(schema: DescMessage): EventContract {
        val options = options(schema, optionExtension(schema.file, ProtocolOptions.EVENT))
        requireNotNull(options) {
            "${schema.typeName} is not a surf-nats event: it has no (${ProtocolOptions.qualified(ProtocolOptions.EVENT)}) option"
        }
        val event = ProtocolResolution.event(
            EventOptions(options.string(ProtocolOptions.SUBJECT), options.boolean(ProtocolOptions.GLOBAL)),
            schema.name,
        )
        requireNoProblems(event.problems(schema.typeName))

        return EventContract(
            typeName = schema.typeName,
            subject = event.subject,
            global = event.global,
            majorVersion = ProtocolNaming.majorVersion(schema.file.proto.`package`, schema.typeName),
        )
    }

    private fun service(service: DescService): ServiceContract {
        services.get(service)?.let { return it }
        val options = options(service, optionExtension(service.file, ProtocolOptions.SERVICE))
        val resolved = ProtocolResolution.service(
            ServiceOptions(
                name = options.string(ProtocolOptions.NAME),
                global = options.boolean(ProtocolOptions.GLOBAL),
                callTimeoutSeconds = options.int(ProtocolOptions.CALL_TIMEOUT_SECONDS),
                recipients = options.recipients(),
                persistent = options.boolean(ProtocolOptions.PERSISTENT),
            ),
            service.name,
        )
        requireNoProblems(resolved.problems(service.typeName))

        return ServiceContract(
            typeName = service.typeName,
            resolved = resolved,
            majorVersion = ProtocolNaming.majorVersion(service.file.proto.`package`, service.typeName),
        ).also { services.set(service, it) }
    }

    private fun resolveMethod(method: DescMethod): MethodContract {
        val serviceDesc = method.parent
        val service = service(serviceDesc)

        val options = options(method, optionExtension(serviceDesc.file, ProtocolOptions.METHOD))
        val resolved = ProtocolResolution.method(
            MethodOptions(
                name = options.string(ProtocolOptions.NAME),
                persistent = options.boolean(ProtocolOptions.PERSISTENT),
                callTimeoutSeconds = options.int(ProtocolOptions.CALL_TIMEOUT_SECONDS),
                recipients = options.recipients(),
            ),
            service.resolved,
            method.name,
            emptyResponse = method.output.typeName == ProtocolFieldRules.EMPTY_TYPE,
            streaming = method.methodKind != "unary",
        )
        requireNoProblems(resolved.problems("${serviceDesc.typeName}.${method.name}"))

        return MethodContract(
            service = service,
            protoName = method.name,
            resolved = resolved,
            input = method.input,
            output = method.output,
        )
    }

    private fun requireNoProblems(problems: List<String>) {
        require(problems.isEmpty()) { problems.joinToString("; ") }
    }

    private fun optionExtension(file: DescFile, name: String): DescExtension? {
        val typeName = ProtocolOptions.qualified(name)
        for (dependency in file.dependencies) {
            for (extension in dependency.extensions) {
                if (extension.typeName == typeName) return extension
            }
        }
        return null
    }

    // The message-typed option [extension] of [element], or null when [element] does not set it.
    private fun options(element: AnyDesc, extension: DescExtension?): ReflectMessage? {
        if (extension == null || !hasOption(element, extension)) return null
        return optionMessage(element, extension)
    }

    // The value of the scalar or enum option field with the proto name [name] when it is set, otherwise null.
    private fun ReflectMessage?.value(name: String): Any? {
        if (this == null) return null
        val field = fields.firstOrNull { it.name == name } ?: return null
        if (!isSet(field)) return null
        return when (val shape = field.shape()) {
            is FieldShape.Scalar -> get(shape.field)
            is FieldShape.Enum -> get(shape.field)
            else -> null
        }
    }

    private fun ReflectMessage?.string(name: String): String? = value(name) as String?

    private fun ReflectMessage?.boolean(name: String): Boolean? = value(name) as Boolean?

    private fun ReflectMessage?.int(name: String): Int? = value(name) as Int?

    private fun ReflectMessage?.recipients(): RecipientsOption? {
        return int(ProtocolOptions.RECIPIENTS)?.let(RecipientsOption::ofNumber)
    }
}
