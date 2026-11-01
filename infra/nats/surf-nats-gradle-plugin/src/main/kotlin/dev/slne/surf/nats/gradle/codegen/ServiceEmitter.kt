package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.*
import dev.slne.surf.nats.protocol.*

/**
 * Kotlin names an rpc must not map to: they clash with the members of [Any].
 */
private val RESERVED_METHOD_NAMES = setOf("equals", "hashCode", "toString")

private const val REQUEST_PARAMETER = "request"

/**
 * Request or response of an rpc: `google.protobuf.Empty` ([shape] `null`) or a generated message.
 */
private class RpcMessage(
    val kotlinType: ClassName,
    val serializer: CodeBlock,
    val shape: MessageShape?,
)

/**
 * Emits a service interface and its `NATS_DESCRIPTOR` companion.
 *
 * Each function takes the rpc request message as its only parameter `request`; rpcs with a `google.protobuf.Empty`
 * request take none.
 */
internal class ServiceEmitter(
    private val model: ContractModel,
    private val visibility: GeneratedVisibility,
) {
    private val problems get() = model.problems

    fun emit(service: Service, className: ClassName): TypeSpec {
        val options = SurfProtoOptions.service(service.options, service.location, problems)
        val resolution = ProtocolResolution.service(options, service.type.toString())

        for (problem in resolution.problems(service.name)) {
            problems.error(service.location, problem)
        }

        if (service.rpcs.isEmpty()) {
            problems.error(
                service.location,
                "service ${service.type} declares no rpcs"
            )
        }

        validateTypeScoping(service)

        val rpcServiceBuilder = TypeSpec.interfaceBuilder(className)
            .applyVisibility(visibility)

        val kotlinNames = HashMap<String, Location>()
        val methodDescriptors = mutableListOf<CodeBlock>()

        for (rpc in service.rpcs) {
            val generatedMethod = method(rpc, resolution, kotlinNames) ?: continue

            rpcServiceBuilder.addFunction(generatedMethod.function)
            methodDescriptors += generatedMethod.descriptor
        }

        val descriptorType = KotlinTypes.NATS_SERVICE_DESCRIPTOR.parameterizedBy(className)

        val descriptorInitializer = buildCodeBlock {
            add("%T(\n", descriptorType)
            withIndent {
                add("protoName = %S,\n", resolution.protoName)
                addListArgument("methods", methodDescriptors)
            }
            add(")")
        }

        val descriptorProperty = PropertySpec.builder(ProtocolDescriptors.FIELD, descriptorType)
            .jvmField()
            .initializer(descriptorInitializer)
            .build()

        val companionObject = TypeSpec.companionObjectBuilder()
            .addProperty(descriptorProperty)
            .build()

        rpcServiceBuilder.addType(companionObject)

        return rpcServiceBuilder.build()
    }

    /**
     * Validates that RPC request and response types do not conflict with RPC names
     * declared in the same service.
     *
     * Protoc and buf resolve unqualified type names within the service scope first.
     * If a type name matches an RPC name, they resolve it to the RPC instead of the
     * intended message type, causing compilation to fail.
     *
     * Wire resolves these names differently and may accept such schemas.
     * This validation ensures compatibility with protoc and buf by requiring
     * package-qualified type names when conflicts occur.
     *
     * @param service the service whose RPC type references are validated
     */
    @Suppress("DestructuringDeclaration")
    private fun validateTypeScoping(service: Service) {
        val rpcNames = service.rpcs.mapTo(HashSet(), Rpc::name)

        for (rpc in Rpc.toElements(service.rpcs)) {
            val types = listOf(rpc.requestType, rpc.responseType)

            for (type in types) {
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

    private class Method(
        val function: FunSpec,
        val descriptor: CodeBlock,
    )

    private fun method(
        rpc: Rpc,
        service: ResolvedService,
        kotlinNames: MutableMap<String, Location>,
    ): Method? {
        val location = rpc.location
        val options = SurfProtoOptions.method(rpc.options, location, problems)
        val resolution = ProtocolResolution.method(
            options,
            service,
            emptyResponse = rpc.responseType == ProtoType.EMPTY,
            streaming = rpc.requestStreaming || rpc.responseStreaming,
        )

        ServiceNames.methodProblem(service.protoName, rpc.name)?.let { problem ->
            problems.error(location, problem)
        }

        for (problem in resolution.problems(rpc.name)) {
            problems.error(location, problem)
        }

        if (resolution.streaming) return null

        val kotlinName = KotlinNaming.lowerCamel(rpc.name)

        if (kotlinName in RESERVED_METHOD_NAMES) {
            problems.error(
                location,
                "rpc ${rpc.name} maps to the Kotlin function '$kotlinName', which clashes with a member of Any",
            )
        }

        kotlinNames.put(kotlinName, location)?.let { previous ->
            problems.error(
                location,
                "rpc ${rpc.name} maps to the Kotlin function '$kotlinName', already used by the rpc at $previous"
            )
        }

        val request = rpcMessage(rpc, rpc.requestType, "request")
        val response = rpcMessage(rpc, rpc.responseType, "response")
        if (request == null || response == null) return null

        val resultKind = resolution.resultKind
        val resultType: TypeName = when (resultKind) {
            ResultKindOption.UNIT -> UNIT
            ResultKindOption.VALUE -> response.kotlinType
            ResultKindOption.ANSWER -> KotlinTypes.ANSWER.parameterizedBy(response.kotlinType)
        }

        val function = FunSpec.builder(kotlinName)
            .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
            .apply {
                if (request.shape != null) {
                    addParameter(REQUEST_PARAMETER, request.kotlinType)
                }

                if (resultKind != ResultKindOption.UNIT) {
                    returns(resultType)
                }
            }
            .build()

        val responseSerializer = if (response.shape == null) {
            CodeBlock.of("null")
        } else {
            response.serializer
        }

        val descriptor = buildCodeBlock {
            add(
                "%T<%T, %T>(\n",
                KotlinTypes.NATS_METHOD_DESCRIPTOR,
                request.kotlinType,
                resultType,
            )

            withIndent {
                add("protoName = %S,\n", rpc.name)
                add("kotlinName = %S,\n", kotlinName)
                add("persistent = %L,\n", resolution.persistent)
                add("callTimeoutSeconds = %L,\n", resolution.callTimeoutSeconds ?: "null")
                add("recipients = %T.%L,\n", KotlinTypes.RECIPIENTS, resolution.recipients.name)
                add(
                    "resultKind = %T.%L,\n",
                    KotlinTypes.NATS_RESULT_KIND,
                    natsResultKind(resultKind)
                )
                add("requestSerializer = %L,\n", request.serializer)
                add("responseSerializer = %L,\n", responseSerializer)
            }

            add(")")
        }

        return Method(function, descriptor)
    }

    /**
     * Resolves the Kotlin type, serializer, and message shape for an RPC request or response.
     *
     * Only generated Protobuf messages and [ProtoType.EMPTY] are supported.
     * Empty messages are represented as Kotlin Unit, while generated messages
     * retain their Kotlin type and associated serializer.
     *
     * Invalid mappings, unknown types, and non-message types are reported
     * through the problem collector.
     *
     * @param rpc the RPC referencing the message type
     * @param protoType the Protobuf request or response type
     * @param role the role of the type ("request" or "response"), used in error messages
     * @return the resolved RPC message, or null if the type cannot be resolved
     */
    private fun rpcMessage(rpc: Rpc, protoType: ProtoType?, role: String): RpcMessage? {
        if (protoType == null) return null

        val mapping = model.mapping(protoType)

        if (protoType == ProtoType.EMPTY) {
            val serializer = mapping?.serializer ?: return null
            return RpcMessage(
                kotlinType = UNIT,
                serializer = CodeBlock.of("%T", serializer),
                shape = null,
            )
        }

        if (mapping != null) {
            problems.error(
                rpc.location,
                "rpc ${rpc.name}: $role type $protoType is mapped to ${mapping.kotlinClass}; only ${ProtoType.EMPTY} " +
                        "or generated messages can be requests and responses",
            )
            return null
        }

        val className = model.generatedClass(protoType)
        val messageType = model.schema.getType(protoType) as? MessageType

        if (className == null) {
            model.reportUnknown(protoType, rpc.location)
            return null
        }

        if (messageType == null) {
            problems.error(
                rpc.location,
                "rpc ${rpc.name}: $role type $protoType must be a message",
            )
            return null
        }

        return RpcMessage(
            className,
            CodeBlock.of("%T.serializer()", className),
            model.shape(messageType, className)
        )
    }

    /**
     * Adds a named list argument to this [CodeBlock.Builder].
     *
     * Generates `name = listOf(...)` with each item on a separate line,
     * or `name = emptyList()` if the list is empty.
     *
     * @param name the name of the argument
     * @param items the code blocks to include as list elements
     * @return this builder for method chaining
     */
    private fun CodeBlock.Builder.addListArgument(
        name: String,
        items: List<CodeBlock>,
    ): CodeBlock.Builder {
        if (items.isEmpty()) {
            return add("%L = emptyList(),\n", name)
        }

        add("%L = listOf(\n", name)
        withIndent {
            for (item in items) {
                add(item)
                add(",\n")
            }
        }

        return add("),\n")
    }

    // Entry names of dev.slne.surf.nats.descriptor.NatsResultKind
    private fun natsResultKind(kind: ResultKindOption): String = when (kind) {
        ResultKindOption.UNIT -> "UNIT"
        ResultKindOption.VALUE -> "VALUE"
        ResultKindOption.ANSWER -> "ANSWER"
    }
}
