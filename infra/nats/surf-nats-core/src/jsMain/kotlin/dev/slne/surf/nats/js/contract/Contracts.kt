package dev.slne.surf.nats.js.contract

import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.DescService
import dev.slne.surf.nats.js.interop.getOrPut
import dev.slne.surf.nats.protocol.EventOptions
import dev.slne.surf.nats.protocol.MethodOptions
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ServiceOptions
import js.collections.WeakMap

/**
 * Resolves protobuf-es descriptors to surf-nats contracts with the shared protocol rules, once per descriptor.
 */
internal object Contracts {
    private val events = WeakMap<DescMessage, EventContract>()
    private val services = WeakMap<DescService, ServiceContract>()
    private val methods = WeakMap<DescMethod, MethodContract>()

    fun event(schema: DescMessage): EventContract = events.getOrPut(schema) { resolveEvent(schema) }

    fun method(method: DescMethod): MethodContract = methods.getOrPut(method) { resolveMethod(method) }

    private fun service(service: DescService): ServiceContract = services.getOrPut(service) { resolveService(service) }

    private fun resolveEvent(schema: DescMessage): EventContract {
        val options = ProtocolOptionReader.event(schema)
        require(options.isSet) {
            "${schema.typeName} is not a surf-nats event: it has no (${ProtocolOptions.qualified(ProtocolOptions.EVENT)}) option"
        }

        val resolved = ProtocolResolution.event(
            EventOptions(
                subject = options.string(ProtocolOptions.SUBJECT),
                global = options.boolean(ProtocolOptions.GLOBAL),
            ),
            schema.name,
        )
        requireNoProblems(resolved.problems(schema.typeName))

        return EventContract(
            typeName = schema.typeName,
            subject = resolved.subject,
            global = resolved.global,
            majorVersion = ProtocolNaming.majorVersion(schema.file.proto.`package`, schema.typeName),
        )
    }

    private fun resolveService(service: DescService): ServiceContract {
        val options = ProtocolOptionReader.service(service)
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
        )
    }

    private fun resolveMethod(method: DescMethod): MethodContract {
        val service = service(method.parent)
        val options = ProtocolOptionReader.method(method)
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
        requireNoProblems(resolved.problems("${method.parent.typeName}.${method.name}"))

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
}
