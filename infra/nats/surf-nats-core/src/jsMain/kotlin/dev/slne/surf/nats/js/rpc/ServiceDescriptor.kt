package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.js.descriptor.PerOwner
import dev.slne.surf.nats.js.descriptor.ProtocolOptionReader
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.DescService
import dev.slne.surf.nats.js.interop.getOrPut
import dev.slne.surf.nats.protocol.MethodOptions
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolOptions
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ResolvedService
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.protocol.ServiceOptions
import js.collections.WeakMap

/**
 * A surf-nats service resolved from its protobuf-es descriptor, with all of its methods validated at once.
 */
internal class ServiceDescriptor private constructor(
    val desc: DescService,
    val resolved: ResolvedService,
    val majorVersion: Int,
) {
    val typeName: String get() = desc.typeName
    val name: String get() = resolved.name
    val global: Boolean get() = resolved.global

    private val names = PerOwner(global, typeName) { owner ->
        ServiceNames(owner, name, majorVersion)
    }

    val methods: List<MethodDescriptor> = resolveMethods()

    // Subjects, queue group and streams of the service under the owning client
    fun names(owner: String?): ServiceNames = names[owner]

    fun method(desc: DescMethod): MethodDescriptor = methods.first { it.desc === desc }

    private fun resolveMethods(): List<MethodDescriptor> {
        val problems = mutableListOf<String>()
        val methods = desc.methods.mapNotNull { method ->
            val options = ProtocolOptionReader.method(method)
            val resolved = ProtocolResolution.method(
                MethodOptions(
                    name = options.string(ProtocolOptions.NAME),
                    persistent = options.boolean(ProtocolOptions.PERSISTENT),
                    callTimeoutSeconds = options.int(ProtocolOptions.CALL_TIMEOUT_SECONDS),
                    recipients = options.recipients(),
                ),
                this.resolved,
                method.name,
                emptyResponse = method.output.typeName == ProtocolFieldRules.EMPTY_TYPE,
                streaming = method.methodKind != "unary",
            )

            val methodProblems = resolved.problems("$typeName.${method.name}")
            if (methodProblems.isEmpty()) {
                MethodDescriptor(method, this, resolved)
            } else {
                problems += methodProblems
                null
            }
        }

        require(problems.isEmpty()) {
            "Invalid surf-nats service $typeName:\n" + problems.joinToString("\n") { " - $it" }
        }

        require(methods.isNotEmpty()) { "$typeName has no service methods" }

        return methods
    }

    companion object {
        private val descriptors = WeakMap<DescService, ServiceDescriptor>()

        fun of(service: DescService): ServiceDescriptor = descriptors.getOrPut(service) { resolve(service) }

        private fun resolve(service: DescService): ServiceDescriptor {
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
            val problems = resolved.problems(service.typeName)
            require(problems.isEmpty()) { problems.joinToString("; ") }

            return ServiceDescriptor(
                service,
                resolved,
                ProtocolNaming.majorVersion(service.file.proto.`package`, service.typeName),
            )
        }
    }
}
