package dev.slne.surf.nats.js.rpc

import dev.slne.surf.nats.js.descriptor.ProtocolOptionReader
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.DescService
import dev.slne.surf.nats.js.interop.getOrPut
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolNaming
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ResolvedService
import dev.slne.surf.nats.protocol.ServiceNames
import js.collections.WeakMap

/**
 * A surf-nats service resolved from its protobuf-es descriptor, with all of its methods validated at once.
 *
 * Subjects and streams derive from the service full name ([names]).
 */
internal class ServiceDescriptor private constructor(
    val desc: DescService,
    val resolved: ResolvedService,
) {
    val typeName: String get() = desc.typeName

    val names = ServiceNames(typeName)

    val methods: List<MethodDescriptor> = resolveMethods()

    fun method(desc: DescMethod): MethodDescriptor = methods.first { it.desc === desc }

    private fun resolveMethods(): List<MethodDescriptor> {
        val problems = mutableListOf<String>()
        val methods = desc.methods.mapNotNull { method ->
            val resolved = ProtocolResolution.method(
                ProtocolOptionReader.method(method),
                this.resolved,
                emptyResponse = method.output.typeName == ProtocolFieldRules.EMPTY_TYPE,
                streaming = method.methodKind != "unary",
            )

            val methodProblems = listOfNotNull(ServiceNames.methodProblem(typeName, method.name)) +
                    resolved.problems("$typeName.${method.name}")
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
            val resolved = ProtocolResolution.service(ProtocolOptionReader.service(service), service.typeName)
            val problems = resolved.problems(service.name)
            require(problems.isEmpty()) { problems.joinToString("; ") }
            ProtocolNaming.majorVersion(service.file.proto.`package`, service.typeName)

            return ServiceDescriptor(service, resolved)
        }
    }
}
