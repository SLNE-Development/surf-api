package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.KotlinNaming
import dev.slne.surf.nats.gradle.model.KotlinTypeResolver
import dev.slne.surf.nats.gradle.model.KotlinTypes
import dev.slne.surf.nats.gradle.model.SurfProtoOptions
import dev.slne.surf.nats.protocol.*

/**
 * Generates Kotlin service interfaces and their NATS descriptors from Protobuf services.
 *
 * Each supported RPC is represented by an abstract suspend function.
 * RPCs accepting `google.protobuf.Empty` have no parameters, while other RPCs
 * receive a single `request` parameter.
 *
 * The generated companion object exposes the service's `NATS_DESCRIPTOR`,
 * containing metadata and serializers for its supported RPCs.
 *
 * Invalid declarations are reported through the type resolver's problem collector.
 */
internal class ServiceEmitter(
    private val types: KotlinTypeResolver,
    private val visibility: GeneratedVisibility,
) {
    companion object {
        private val RESERVED_METHOD_NAMES = setOf("equals", "hashCode", "toString")
        private const val REQUEST_PARAMETER = "request"
    }

    private val problems get() = types.problems

    /**
     * Generates the Kotlin interface for a Protobuf service.
     *
     * Resolves and validates the service, generates its supported RPC functions,
     * and attaches the corresponding NATS service descriptor.
     *
     * @param service the Protobuf service to generate
     * @return the generated Kotlin interface specification
     */
    fun emit(service: Service): TypeSpec {
        val className = checkNotNull(types.generatedClass(service.type)) {
            "Could not find the generated class name for '${service.type}'"
        }
        val resolution = resolveService(service)

        validateService(service, resolution)

        return TypeSpec.interfaceBuilder(className)
            .applyVisibility(visibility)
            .apply {
                val descriptors = addMethods(service, resolution)
                addType(createDescriptorCompanion(className, resolution, descriptors))
            }
            .build()
    }

    /**
     * Resolves the NATS configuration of a Protobuf service.
     *
     * Reads the service's custom Protobuf options and applies the protocol's
     * resolution rules.
     *
     * @param service the service whose configuration is resolved
     * @return the resolved service configuration
     */
    private fun resolveService(service: Service): ResolvedService {
        val options = SurfProtoOptions.service(service.options, service.location, problems)
        return ProtocolResolution.service(options, service.type.toString())
    }

    /**
     * Validates a service and reports any detected problems.
     *
     * Checks the resolved protocol configuration, rejects services without RPCs,
     * and verifies that RPC type references do not conflict with RPC names.
     *
     * @param service the service to validate
     * @param resolution the resolved service configuration
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
     * Generates and adds all supported RPC functions to this interface builder.
     *
     * Tracks generated Kotlin function names to detect collisions between RPCs.
     * RPCs that cannot be generated are omitted, while their validation errors
     * are reported by the individual method generator.
     *
     * @param service the service containing the RPC declarations
     * @param resolution the resolved service configuration
     * @return the descriptors of successfully generated RPC functions
     */
    private fun TypeSpec.Builder.addMethods(
        service: Service,
        resolution: ResolvedService,
    ): List<CodeBlock> {
        val kotlinNames = HashMap<String, Location>()
        val descriptors = mutableListOf<CodeBlock>()

        for (rpc in service.rpcs) {
            val (function, descriptor) = createMethod(rpc, resolution, kotlinNames) ?: continue

            this.addFunction(function)
            descriptors.add(descriptor)
        }

        return descriptors
    }

    /**
     * Generates the companion object containing the NATS service descriptor.
     *
     * The descriptor stores the resolved Protobuf service name and the metadata
     * of all successfully generated RPC functions.
     *
     * @param className the generated service interface type
     * @param resolution the resolved service configuration
     * @param methods the generated RPC descriptor expressions
     * @return the companion object specification
     */
    private fun createDescriptorCompanion(
        className: ClassName,
        resolution: ResolvedService,
        methods: List<CodeBlock>,
    ): TypeSpec {
        val descriptorType = KotlinTypes.NATS_SERVICE_DESCRIPTOR
            .parameterizedBy(className)

        val descriptorInitializer = buildCodeBlock {
            add("%T(\n", descriptorType)
            withIndent {
                add("protoName = %S,\n", resolution.protoName)
                addListArgument("methods", methods)
            }
            add(")")
        }

        val descriptorProperty = PropertySpec.builder(ProtocolDescriptors.FIELD, descriptorType)
            .jvmField()
            .initializer(descriptorInitializer)
            .build()

        return TypeSpec.companionObjectBuilder()
            .addProperty(descriptorProperty)
            .build()
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
     *
     * @param service the service whose RPC type references are validated
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
     * Generates a Kotlin function and its NATS descriptor for an RPC.
     *
     * Resolves protocol options, validates the generated Kotlin name, and
     * resolves the request and response message types before generating code.
     *
     * Streaming RPCs are not generated. Invalid request or response types
     * also prevent generation, with errors reported through the problem collector.
     *
     * @param rpc the RPC declaration to process
     * @param service the resolved configuration of the containing service
     * @param kotlinNames previously generated Kotlin names and their source locations
     * @return the generated function and descriptor, or `null` if generation is unsupported or fails
     */
    private fun createMethod(
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

        val resultKind = resolution.resultKind
        val resultType = resultType(resultKind, response.kotlinType)

        return Method(
            function = methodFunction(rpc, kotlinName, request, resultKind, resultType),
            descriptor = methodDescriptor(
                rpcName = rpc.name,
                kotlinName = kotlinName,
                request = request,
                response = response,
                resultType = resultType,
                resultKind = resultKind,
                persistent = resolution.persistent,
                callTimeout = CodeBlock.of("%L", resolution.callTimeoutSeconds ?: "null"),
                recipients = resolution.recipients.name,
            ),
        )
    }

    /**
     * Validates a generated Kotlin function name and records its source location.
     *
     * Reports collisions with members inherited from [Any] and names already
     * used by other RPCs in the same service.
     *
     * The name is recorded even when a collision is detected, allowing subsequent
     * RPCs to be checked against it.
     *
     * @param rpc the RPC associated with the generated name
     * @param kotlinName the generated Kotlin function name
     * @param kotlinNames previously encountered function names and their locations
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
     * Determines the Kotlin return type for an RPC result kind.
     *
     * Unit results use Kotlin [Unit], value results use the response message
     * type directly, and answer results wrap that type in the NATS answer type.
     *
     * @param kind the configured RPC result kind
     * @param responseType the resolved Kotlin response message type
     * @return the Kotlin return type used by the function and descriptor
     */
    private fun resultType(
        kind: ResultKindOption,
        responseType: ClassName,
    ): TypeName = when (kind) {
        ResultKindOption.UNIT -> UNIT
        ResultKindOption.VALUE -> responseType
        ResultKindOption.ANSWER -> KotlinTypes.ANSWER.parameterizedBy(responseType)
    }

    /**
     * Generates the abstract suspend function representing an RPC.
     *
     * Adds a request parameter unless the RPC accepts `google.protobuf.Empty`.
     * An explicit return type is generated only for non-Unit results.
     *
     * @param rpc the original RPC declaration
     * @param kotlinName the validated Kotlin function name
     * @param request the resolved request message
     * @param resultKind the configured result kind
     * @param resultType the resolved Kotlin return type
     * @return the generated function specification
     */
    private fun methodFunction(
        rpc: Rpc,
        kotlinName: String,
        request: RpcMessage,
        resultKind: ResultKindOption,
        resultType: TypeName,
    ): FunSpec {
        return FunSpec.builder(kotlinName)
            .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
            .apply {
                if (rpc.requestType != ProtoType.EMPTY) {
                    addParameter(REQUEST_PARAMETER, request.kotlinType)
                }

                if (resultKind != ResultKindOption.UNIT) {
                    returns(resultType)
                }
            }
            .build()
    }

    /**
     * Generates the NATS method descriptor expression for an RPC.
     *
     * Includes protocol and Kotlin names, delivery configuration, result kind,
     * and request and response serializers.
     *
     * Empty responses are represented by a null response serializer because
     * they do not contain a response payload.
     *
     * @param rpcName the original Protobuf RPC name
     * @param kotlinName the generated Kotlin function name
     * @param request the resolved request message
     * @param response the resolved response message
     * @param resultType the generated Kotlin result type
     * @param resultKind the configured RPC result kind
     * @param persistent whether the RPC uses persistent messaging
     * @param callTimeout the generated call timeout expression
     * @param recipients the configured recipients enum entry name
     * @return the generated method descriptor expression
     */
    private fun methodDescriptor(
        rpcName: String,
        kotlinName: String,
        request: RpcMessage,
        response: RpcMessage,
        resultType: TypeName,
        resultKind: ResultKindOption,
        persistent: Boolean,
        callTimeout: CodeBlock,
        recipients: String,
    ): CodeBlock {
        val responseSerializer = if (response.kotlinType == UNIT) {
            CodeBlock.of("null")
        } else {
            response.serializer
        }

        return buildCodeBlock {
            add("%T<%T, %T>(\n", KotlinTypes.NATS_METHOD_DESCRIPTOR, request.kotlinType, resultType)
            withIndent {
                add("protoName = %S,\n", rpcName)
                add("kotlinName = %S,\n", kotlinName)
                add("persistent = %L,\n", persistent)
                add("callTimeoutSeconds = %L,\n", callTimeout)
                add("recipients = %T.%L,\n", KotlinTypes.RECIPIENTS, recipients)
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
     * @param rpc the RPC referencing the message type
     * @param protoType the Protobuf request or response type
     * @param role the message role, used when reporting validation errors
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
     * @param rpc the RPC referencing the message
     * @param protoType the Protobuf type to resolve
     * @param role the message role, used when reporting validation errors
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

    /**
     * Appends a named list argument to this code block builder.
     *
     * Each element is placed on a separate line. Empty collections are emitted
     * as `emptyList()` instead of `listOf()`.
     *
     * @param name the generated argument name
     * @param items the code blocks representing the list elements
     */
    private fun CodeBlock.Builder.addListArgument(name: String, items: List<CodeBlock>) {
        if (items.isEmpty()) {
            add("%L = emptyList(),\n", name)
            return
        }

        add("%L = listOf(\n", name)
        withIndent { add(items.joinToCode(",\n", suffix = ",\n")) }
        add("),\n")
    }

    /**
     * Maps a protocol result kind to its runtime NATS result kind enum entry.
     *
     * @param kind the protocol-level result kind
     * @return the matching entry name in `NatsResultKind`
     */
    private fun natsResultKind(kind: ResultKindOption): String = when (kind) {
        ResultKindOption.UNIT -> "UNIT"
        ResultKindOption.VALUE -> "VALUE"
        ResultKindOption.ANSWER -> "ANSWER"
    }

    /**
     * Holds a generated RPC function and its corresponding descriptor expression.
     */
    private data class Method(val function: FunSpec, val descriptor: CodeBlock)

    /**
     * Represents a resolved RPC request or response message.
     *
     * Contains the Kotlin message type and the expression used to obtain
     * its serializer. `google.protobuf.Empty` is represented by Kotlin [Unit].
     */
    private class RpcMessage(val kotlinType: ClassName, val serializer: CodeBlock)
}
