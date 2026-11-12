package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service
import dev.slne.surf.nats.gradle.contract.ContractMethod
import dev.slne.surf.nats.gradle.contract.ContractResolver
import dev.slne.surf.nats.gradle.contract.ResolvedContract
import dev.slne.surf.nats.gradle.contract.RpcMessage
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.KotlinTypeResolver
import dev.slne.surf.nats.gradle.model.KotlinTypes
import dev.slne.surf.nats.protocol.ProtocolDescriptors
import dev.slne.surf.nats.protocol.ResultKindOption

/**
 * Generates Kotlin service interfaces and their NATS descriptors from the services of a [ResolvedContract].
 *
 * Reports nothing: [ContractResolver] has checked every service before emission starts. The runtime relies on this
 * generated shape:
 *
 * - The service is an interface with the configured visibility and one `abstract suspend fun` per
 *   [ResolvedContract.methods] entry, in rpc declaration order.
 * - A function takes a single `request` parameter, or none when the rpc request is `google.protobuf.Empty`.
 * - The return type follows the resolved result kind: none for `UNIT`, the response type for `VALUE`, and
 *   `Answer<Response>` for `ANSWER`.
 * - The companion has `@JvmField val NATS_DESCRIPTOR: NatsServiceDescriptor<Service>` with the proto service name
 *   and one `NatsMethodDescriptor` per function. A method descriptor carries the proto and Kotlin names, the
 *   resolved `persistent`, `callTimeoutSeconds` (`null` for the default), `recipients` and `resultKind`, and the
 *   request and response serializers. The response serializer is `null` when the response is
 *   `google.protobuf.Empty`.
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
     * Generates the interface of [service] with its functions and descriptor companion.
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

    private fun resultType(
        kind: ResultKindOption,
        responseType: ClassName,
    ): TypeName = when (kind) {
        ResultKindOption.UNIT -> UNIT
        ResultKindOption.VALUE -> responseType
        ResultKindOption.ANSWER -> KotlinTypes.ANSWER.parameterizedBy(responseType)
    }

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
     * Generates the `NatsMethodDescriptor` expression of an rpc.
     *
     * The response serializer is `null` for a `google.protobuf.Empty` response, which has no payload.
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
     * Appends `name = listOf(...)` with one item per line, or `name = emptyList()`.
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

    private fun natsResultKind(kind: ResultKindOption): String = when (kind) {
        ResultKindOption.UNIT -> "UNIT"
        ResultKindOption.VALUE -> "VALUE"
        ResultKindOption.ANSWER -> "ANSWER"
    }

    private data class Method(val function: FunSpec, val descriptor: CodeBlock)
}
