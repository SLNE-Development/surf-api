package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.UNIT
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.model.KotlinNaming
import dev.slne.surf.nats.gradle.model.KotlinTypeResolver
import dev.slne.surf.nats.gradle.model.SurfProtoOptions
import dev.slne.surf.nats.protocol.ResolvedService
import dev.slne.surf.nats.protocol.ServiceNames

/**
 * Resolves the methods of generated services.
 */
internal class ServiceResolver(private val types: KotlinTypeResolver) {
    private val problems get() = types.problems

    /**
     * Returns the methods of [service] in rpc order and reports options, names and payload types that cannot be
     * generated.
     *
     * The protocol rules of each rpc come from `ResolvedMethod.problems`: streaming rpcs are rejected, persistent
     * rpcs cannot address `ALL_INSTANCES`, and a persistent rpc without a result must not have a call timeout above
     * the 120-second duplicate window. Streaming rpcs have no [ContractMethod]. The other violations are reported
     * and the method is kept.
     */
    fun resolve(service: Service): List<ContractMethod> {
        val resolution = SurfProtoOptions.service(service, problems)
        validateService(service, resolution)

        val kotlinNames = HashMap<String, Location>()
        return service.rpcs.mapNotNull { method(it, resolution, kotlinNames) }
    }

    /**
     * Validates a service and reports any detected problems.
     *
     * Checks the resolved protocol configuration, rejects services without RPCs,
     * and verifies that RPC type references do not conflict with RPC names.
     */
    private fun validateService(service: Service, resolution: ResolvedService) {
        for (problem in resolution.problems(service.name)) {
            problems.error(service.location, problem)
        }

        if (service.rpcs.isEmpty()) {
            problems.error(service.location, "service ${service.type} declares no rpcs")
        }

        validateTypeScoping(service)
    }

    /**
     * Validates RPC type references against names declared in the same service.
     *
     * Protoc and buf resolve unqualified type names within the service scope
     * before looking for message types. Consequently, a type reference starting
     * with an RPC name may resolve to that RPC rather than the intended message.
     *
     * Wire handles these references differently, so this validation ensures
     * compatibility with protoc and buf by requiring package-qualified names
     * for conflicting references.
     */
    @Suppress("DestructuringDeclaration")
    private fun validateTypeScoping(service: Service) {
        val rpcNames = service.rpcs.mapTo(HashSet(), Rpc::name)

        for (rpc in Rpc.toElements(service.rpcs)) {
            for (type in listOf(rpc.requestType, rpc.responseType)) {
                val typeName = type.substringBefore('.')
                if (typeName !in rpcNames) continue

                problems.error(
                    rpc.location,
                    "rpc ${rpc.name}: type '$type' conflicts with RPC '$typeName' in the same service. " +
                            "Use a package-qualified type name.",
                )
            }
        }
    }

    /**
     * Resolves the options, the Kotlin name and the request and response types of an RPC.
     *
     * Streaming RPCs are not generated. Invalid request or response types
     * also prevent generation, with errors reported through the problem collector.
     *
     * @return the resolved method, or `null` if generation is unsupported or fails
     */
    private fun method(
        rpc: Rpc,
        service: ResolvedService,
        kotlinNames: MutableMap<String, Location>,
    ): ContractMethod? {
        val location = rpc.location
        val resolution = SurfProtoOptions.method(rpc, service, problems)

        ServiceNames.methodProblem(service.protoName, rpc.name)
            ?.let { problems.error(location, it) }

        for (problem in resolution.problems(rpc.name)) {
            problems.error(location, problem)
        }

        if (resolution.streaming) return null

        val kotlinName = KotlinNaming.lowerCamel(rpc.name)
        validateMethodName(rpc, kotlinName, kotlinNames)

        val request = rpcMessage(rpc, rpc.requestType, "request")
        val response = rpcMessage(rpc, rpc.responseType, "response")
        if (request == null || response == null) return null

        return ContractMethod(rpc, kotlinName, request, response, resolution)
    }

    /**
     * Validates a generated Kotlin function name and records its source location.
     *
     * Reports collisions with members inherited from [Any] and names already
     * used by other RPCs in the same service.
     *
     * The name is recorded even when a collision is detected, allowing subsequent
     * RPCs to be checked against it.
     */
    private fun validateMethodName(
        rpc: Rpc,
        kotlinName: String,
        kotlinNames: MutableMap<String, Location>,
    ) {
        if (kotlinName in RESERVED_METHOD_NAMES) {
            problems.error(
                rpc.location,
                "rpc ${rpc.name} maps to the Kotlin function '$kotlinName', which clashes with a member of Any"
            )
        }

        kotlinNames.put(kotlinName, rpc.location)?.let { previous ->
            problems.error(
                rpc.location,
                "rpc ${rpc.name} maps to the Kotlin function '$kotlinName', already used by the rpc at $previous"
            )
        }
    }

    /**
     * Resolves the Kotlin type and serializer of an RPC request or response.
     *
     * Only generated Protobuf messages and `google.protobuf.Empty` are supported.
     * Empty messages are represented as Kotlin [Unit].
     *
     * Rejects custom mappings for non-empty messages and delegates generated
     * message resolution to [generatedMessage].
     *
     * @return the resolved message, or `null` if the type cannot be resolved
     */
    private fun rpcMessage(rpc: Rpc, protoType: ProtoType?, role: String): RpcMessage? {
        if (protoType == null) return null

        val mapping = types.mapping(protoType)

        if (protoType == ProtoType.EMPTY) {
            val serializer = mapping?.serializer ?: return null
            return RpcMessage(UNIT, CodeBlock.of("%T", serializer))
        }

        if (mapping != null) {
            problems.error(
                rpc.location,
                "rpc ${rpc.name}: $role type $protoType is mapped to ${mapping.kotlinClass}; only ${ProtoType.EMPTY} " +
                        "or generated messages can be requests and responses",
            )

            return null
        }

        return generatedMessage(rpc, protoType, role)
    }

    /**
     * Resolves a generated Protobuf message used by an RPC.
     *
     * Ensures that the referenced type has a generated Kotlin class and is
     * declared as a Protobuf message rather than another schema type.
     *
     * @return the resolved message and serializer, or `null` if validation fails
     */
    private fun generatedMessage(rpc: Rpc, protoType: ProtoType, role: String): RpcMessage? {
        val className = types.generatedClass(protoType)

        if (className == null) {
            types.reportUnknown(protoType, rpc.location)
            return null
        }

        if (types.schema.getType(protoType) !is MessageType) {
            problems.error(rpc.location, "rpc ${rpc.name}: $role type $protoType must be a message")
            return null
        }

        return RpcMessage(className, CodeBlock.of("%T.serializer()", className))
    }

    private companion object {
        val RESERVED_METHOD_NAMES = setOf("equals", "hashCode", "toString")
    }
}
