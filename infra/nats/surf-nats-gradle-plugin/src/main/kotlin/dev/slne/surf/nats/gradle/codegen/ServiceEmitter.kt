package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.contract.ContractMethod
import dev.slne.surf.nats.gradle.contract.ContractResolver
import dev.slne.surf.nats.gradle.contract.ResolvedContract
import dev.slne.surf.nats.gradle.contract.RpcMessage
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.KotlinTypeResolver
import dev.slne.surf.nats.gradle.model.KotlinTypes
import dev.slne.surf.nats.protocol.*

/**
 * Generates Kotlin service interfaces and their NATS descriptors from the services of a [ResolvedContract].
 *
 * Each supported RPC is represented by an abstract suspend function.
 * RPCs accepting `google.protobuf.Empty` have no parameters, while other RPCs
 * receive a single `request` parameter.
 *
 * The generated companion object exposes the service's `NATS_DESCRIPTOR`,
 * containing metadata and serializers for its supported RPCs.
 *
 * Reports nothing: [ContractResolver] has checked every service before emission starts.
 */
internal class ServiceEmitter(
    private val types: KotlinTypeResolver,
    private val contract: ResolvedContract,
    private val visibility: GeneratedVisibility,
) {
    companion object {
        private const val REQUEST_PARAMETER = "request"
    }

    /**
     * Generates the Kotlin interface for a Protobuf service.
     *
     * Generates the functions of its resolved RPCs and attaches the corresponding NATS service descriptor.
     *
     * @param service the Protobuf service to generate
     * @return the generated Kotlin interface specification
     */
    fun emit(service: Service): TypeSpec {
        val className = checkNotNull(types.generatedClass(service.type)) {
            "Could not find the generated class name for '${service.type}'"
        }
        val methods = contract.methods(service).map(::createMethod)

        return TypeSpec.interfaceBuilder(className)
            .applyVisibility(visibility)
            .addFunctions(methods.map { it.function })
            .addType(createDescriptorCompanion(className, service.type.toString(), methods.map { it.descriptor }))
            .build()
    }

    /**
     * Generates the companion object containing the NATS service descriptor.
     *
     * The descriptor stores the Protobuf service name and the metadata
     * of all successfully generated RPC functions.
     *
     * @param className the generated service interface type
     * @param protoName the fully qualified Protobuf name of the service
     * @param methods the generated RPC descriptor expressions
     * @return the companion object specification
     */
    private fun createDescriptorCompanion(
        className: ClassName,
        protoName: String,
        methods: List<CodeBlock>,
    ): TypeSpec {
        val descriptorType = KotlinTypes.NATS_SERVICE_DESCRIPTOR
            .parameterizedBy(className)

        val descriptorInitializer = buildCodeBlock {
            add("%T(\n", descriptorType)
            withIndent {
                add("protoName = %S,\n", protoName)
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
     * Generates a Kotlin function and its NATS descriptor for an RPC.
     *
     * @param method the resolved RPC
     * @return the generated function and descriptor
     */
    private fun createMethod(method: ContractMethod): Method {
        val resolution = method.resolution
        val resultKind = resolution.resultKind
        val resultType = resultType(resultKind, method.response.kotlinType)

        return Method(
            function = methodFunction(method.rpc, method.kotlinName, method.request, resultKind, resultType),
            descriptor = methodDescriptor(
                rpcName = method.rpc.name,
                kotlinName = method.kotlinName,
                request = method.request,
                response = method.response,
                resultType = resultType,
                resultKind = resultKind,
                persistent = resolution.persistent,
                callTimeout = CodeBlock.of("%L", resolution.callTimeoutSeconds ?: "null"),
                recipients = resolution.recipients.name,
            ),
        )
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
}
