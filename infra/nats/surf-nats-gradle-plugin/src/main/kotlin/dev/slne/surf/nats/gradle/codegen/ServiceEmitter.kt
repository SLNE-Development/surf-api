package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Rpc
import com.squareup.wire.schema.Service
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.ContractModel
import dev.slne.surf.nats.gradle.model.KotlinNaming
import dev.slne.surf.nats.gradle.model.KotlinTypes
import dev.slne.surf.nats.gradle.model.MessageShape
import dev.slne.surf.nats.gradle.model.SurfOptions
import dev.slne.surf.nats.protocol.ProtocolDescriptors
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ResolvedService
import dev.slne.surf.nats.protocol.ResultKindOption

/**
 * Kotlin names an rpc must not map to: they clash with the members of [Any].
 */
private val RESERVED_METHOD_NAMES = setOf("equals", "hashCode", "toString")

/**
 * Request or response of an rpc: `google.protobuf.Empty` ([shape] `null`) or a generated message.
 */
private class RpcMessage(val kotlinType: ClassName, val serializer: CodeBlock, val shape: MessageShape?)

/**
 * Emits a service interface with flattened rpc parameters and its `NATS_DESCRIPTOR` companion.
 */
internal class ServiceEmitter(
    private val model: ContractModel,
    private val visibility: GeneratedVisibility,
    private val services: MutableMap<ServiceKey, Location>,
) {
    private val problems get() = model.problems

    /**
     * Emits [service]; file-private constants its descriptor uses are added to [constants].
     */
    fun emit(service: Service, className: ClassName, majorVersion: Int?, constants: FileConstants): TypeSpec {
        val resolved = ProtocolResolution.service(SurfOptions.service(service.options, service.location, problems), service.name)
        val name = resolved.name
        val global = resolved.global
        for (problem in resolved.problems(service.type.toString())) problems.error(service.location, problem)
        if (majorVersion != null) {
            services.put(ServiceKey(global, name, majorVersion), service.location)?.let { previous ->
                problems.error(
                    service.location,
                    "service ${service.type} resolves to the ${if (global) "global" else "prefixed"} name '$name' " +
                            "of v$majorVersion, already used by the service at $previous",
                )
            }
        }
        if (service.rpcs.isEmpty()) problems.error(service.location, "service ${service.type} declares no rpcs")
        validateTypeScoping(service)

        val builder = TypeSpec.interfaceBuilder(className).applyVisibility(visibility)

        val tokens = HashMap<String, Location>()
        val kotlinNames = HashMap<String, Location>()
        val descriptors = mutableListOf<CodeBlock>()
        var uncheckedCasts = false
        for (rpc in service.rpcs) {
            val method = method(rpc, resolved, tokens, kotlinNames, constants) ?: continue
            builder.addFunction(method.function)
            descriptors += method.descriptor
            uncheckedCasts = uncheckedCasts || method.uncheckedCasts
        }

        val descriptorType = KotlinTypes.NATS_SERVICE_DESCRIPTOR.parameterizedBy(className)
        val initializer = CodeBlock.builder()
            .add("%T(\n", descriptorType)
            .indent()
            .add("protoName = %S,\n", service.type.toString())
            .add("name = %S,\n", name)
            .add("global = %L,\n", global)
            .add("majorVersion = %L,\n", majorVersion ?: 0)
            .addListArgument("methods", descriptors)
            .unindent()
            .add(")")

        val property = PropertySpec.builder(ProtocolDescriptors.FIELD, descriptorType)
            .addAnnotation(JvmField::class)
            .initializer(initializer.build())
        if (uncheckedCasts) {
            property.addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
        }
        builder.addType(TypeSpec.companionObjectBuilder().addProperty(property.build()).build())
        return builder.build()
    }

    // protoc and buf look a type name up in the service scope first: an unqualified request or response type whose first
    // component names an rpc of the same service resolves to that rpc and the file fails there, while wire resolves it
    // to the message.
    private fun validateTypeScoping(service: Service) {
        val rpcNames = service.rpcs.mapTo(HashSet(), Rpc::name)
        for (rpc in Rpc.toElements(service.rpcs)) {
            for (type in listOf(rpc.requestType, rpc.responseType)) {
                val scopeName = type.substringBefore('.')
                if (scopeName in rpcNames) {
                    problems.error(
                        rpc.location,
                        "rpc ${rpc.name}: protoc resolves the type '$type' to the rpc '$scopeName' of this service; " +
                                "use the package-qualified type name",
                    )
                }
            }
        }
    }

    private class Method(val function: FunSpec, val descriptor: CodeBlock, val uncheckedCasts: Boolean)

    private fun method(
        rpc: Rpc,
        service: ResolvedService,
        tokens: MutableMap<String, Location>,
        kotlinNames: MutableMap<String, Location>,
        constants: FileConstants,
    ): Method? {
        val location = rpc.location
        val resolved = ProtocolResolution.method(
            SurfOptions.method(rpc.options, location, problems),
            service,
            rpc.name,
            emptyResponse = rpc.responseType == ProtoType.EMPTY,
            streaming = rpc.requestStreaming || rpc.responseStreaming,
        )
        for (problem in resolved.problems(rpc.name)) problems.error(location, problem)
        if (resolved.streaming) return null

        val token = resolved.name
        tokens.put(token, location)?.let { previous ->
            problems.error(location, "rpc ${rpc.name} resolves to the method name '$token', already used by the rpc at $previous")
        }
        val kotlinName = KotlinNaming.lowerCamel(rpc.name)
        if (kotlinName in RESERVED_METHOD_NAMES) {
            problems.error(location, "rpc ${rpc.name} maps to the Kotlin function '$kotlinName', which clashes with a member of Any")
        }
        kotlinNames.put(kotlinName, location)?.let { previous ->
            problems.error(location, "rpc ${rpc.name} maps to the Kotlin function '$kotlinName', already used by the rpc at $previous")
        }

        val request = rpcMessage(rpc, rpc.requestType, "request")
        val response = rpcMessage(rpc, rpc.responseType, "response")
        if (request == null || response == null) return null

        val resultKind = resolved.resultKind
        val resultType: TypeName = when (resultKind) {
            ResultKindOption.UNIT -> UNIT
            ResultKindOption.VALUE -> response.kotlinType
            ResultKindOption.ANSWER -> KotlinTypes.ANSWER.parameterizedBy(response.kotlinType)
        }

        val properties = request.shape?.properties.orEmpty()
        val function = FunSpec.builder(kotlinName).addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
        for (property in properties) {
            function.addParameter(
                ParameterSpec.builder(property.name, property.parameterType)
                    .apply { property.default?.let { defaultValue(it) } }
                    .build()
            )
        }
        if (resultKind != ResultKindOption.UNIT) function.returns(resultType)

        val descriptor = CodeBlock.builder()
            .add("%T<%T, %T>(\n", KotlinTypes.NATS_METHOD_DESCRIPTOR, request.kotlinType, resultType)
            .indent()
            .add("protoName = %S,\n", rpc.name)
            .add("name = %S,\n", token)
            .add("kotlinName = %S,\n", kotlinName)
            .add("persistent = %L,\n", resolved.persistent)
            .add("callTimeoutSeconds = %L,\n", resolved.callTimeoutSeconds ?: "null")
            .add("recipients = %T.%L,\n", KotlinTypes.RECIPIENTS, resolved.recipients.name)
            .add("resultKind = %T.%L,\n", KotlinTypes.NATS_RESULT_KIND, natsResultKind(resultKind))
            .addListArgument(
                "parameters",
                properties.map { property ->
                    CodeBlock.of(
                        "%T(name = %S, type = %T::class, nullable = %L)",
                        KotlinTypes.NATS_PARAMETER,
                        property.name,
                        property.kClass,
                        property.nullable,
                    )
                },
            )
        descriptor.add("requestSerializer = ").add(request.serializer).add(",\n")
        descriptor.add("responseSerializer = ")
        if (response.shape == null) descriptor.add("null") else descriptor.add(response.serializer)
        descriptor.add(",\n")

        when {
            request.shape == null -> descriptor.add("toRequest = { },\n")
            properties.isEmpty() -> descriptor.add("toRequest = { %N },\n", constants.instance(request.kotlinType))
            else -> {
                descriptor.add("toRequest = { a -> %T(", request.kotlinType)
                properties.forEachIndexed { index, property ->
                    if (index > 0) descriptor.add(", ")
                    descriptor.add("%N = a[%L] as %T", property.name, index, property.parameterType)
                }
                descriptor.add(") },\n")
            }
        }
        if (properties.isEmpty()) {
            descriptor.add("toArguments = { %N },\n", constants.noArguments())
        } else {
            descriptor.add("toArguments = { r -> arrayOf<%T>(", ANY.copy(nullable = true))
            properties.forEachIndexed { index, property ->
                if (index > 0) descriptor.add(", ")
                descriptor.add("r.%N", property.name)
            }
            descriptor.add(") },\n")
        }
        descriptor.unindent().add(")")

        val uncheckedCasts = properties.any { it.parameterType.isGenericCast() }
        return Method(function.build(), descriptor.build(), uncheckedCasts)
    }

    private fun rpcMessage(rpc: Rpc, protoType: ProtoType?, role: String): RpcMessage? {
        protoType ?: return null
        if (protoType == ProtoType.EMPTY) {
            val serializer = model.mapping(protoType)?.serializer ?: return null
            return RpcMessage(UNIT, CodeBlock.of("%T", serializer), null)
        }
        val mapped = model.mapping(protoType)
        if (mapped != null) {
            problems.error(
                rpc.location,
                "rpc ${rpc.name}: $role type $protoType is mapped to ${mapped.kotlinClass}; only ${ProtoType.EMPTY} " +
                        "or generated messages can be requests and responses",
            )
            return null
        }
        val className = model.generatedClass(protoType)
        val messageType = model.schema.getType(protoType) as? MessageType
        if (className == null || messageType == null) {
            if (className == null) model.reportUnknown(protoType, rpc.location)
            else problems.error(rpc.location, "rpc ${rpc.name}: $role type $protoType must be a message")
            return null
        }
        return RpcMessage(className, CodeBlock.of("%T.serializer()", className), model.shape(messageType, className))
    }

    private fun TypeName.isGenericCast(): Boolean = this is ParameterizedTypeName

    /**
     * Adds the named argument `[name] = listOf(...)` with one [items] entry per line, or `[name] = emptyList()`.
     */
    private fun CodeBlock.Builder.addListArgument(name: String, items: List<CodeBlock>): CodeBlock.Builder {
        if (items.isEmpty()) return add("%L = emptyList(),\n", name)
        add("%L = listOf(\n", name).indent()
        for (item in items) add(item).add(",\n")
        return unindent().add("),\n")
    }

    // Entry names of dev.slne.surf.nats.descriptor.NatsResultKind
    private fun natsResultKind(kind: ResultKindOption): String = when (kind) {
        ResultKindOption.UNIT -> "UNIT"
        ResultKindOption.VALUE -> "VALUE"
        ResultKindOption.ANSWER -> "ANSWER"
    }
}
