package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsFormat
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.WireSerializers
import dev.slne.surf.nats.internal.NATS_CLIENT_NAME_PATTERN
import dev.slne.surf.nats.jetstream.PersistentStreams
import dev.slne.surf.nats.subject.Subjects
import dev.slne.surf.nats.util.isSuspend
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.Duration.Companion.seconds

@InternalNatsApi
internal class ServiceDescriptor(
    val type: Class<*>,
    val name: String,
    val methods: List<MethodDescriptor>,
    val presenceSubject: String?,
    val streams: PersistentStreams?,
    val responderGroup: String, // queue group of ONE methods and durable consumer of persistent methods
) {
    companion object {
        private const val RPC = $$"$rpc"
        private const val PERSIST = $$"$persist"
        private const val PRESENCE = $$"$presence"
        private const val DLQ = $$"$dlq"

        private val SERVICE_NAME = NATS_CLIENT_NAME_PATTERN.toRegex()

        fun of(type: Class<*>, client: NatsClientImpl): ServiceDescriptor {
            require(type.isInterface) { notAnInterfaceMessage(type) }
            val annotation = requireNotNull(type.getAnnotation(NatsService::class.java)) {
                "${type.name} must be annotated with @NatsService"
            }
            val servicePersistent = type.getAnnotation(NatsPersistent::class.java)?.enabled ?: false
            val serviceResponders =
                type.getAnnotation(NatsResponders::class.java)?.mode ?: ResponderMode.ONE
            val serviceFormat = type.getAnnotation(NatsFormat::class.java)?.format
            val serviceTimeoutSeconds = type.getAnnotation(NatsTimeout::class.java)?.seconds
            val serviceTimeout = serviceTimeoutSeconds?.takeIf { it > 0 }?.seconds

            val codec = client.codec
            val serviceName = annotation.name.ifEmpty { Subjects.kebabCase(type.simpleName) }

            val problems = ObjectArrayList<String>()
            if (!SERVICE_NAME.matches(serviceName)) {
                problems.add("Service name '$serviceName' must be kebab-case ([a-z0-9] separated by '-')")
            }
            if (serviceName == "dlq" || serviceName.endsWith("-dlq")) {
                problems.add("Service name '$serviceName' is reserved, names ending in 'dlq' collide with dead letter streams")
            }
            if (serviceTimeoutSeconds != null && serviceTimeoutSeconds <= 0) {
                problems.add("@NatsTimeout on ${type.simpleName} must be positive, was $serviceTimeoutSeconds")
            }

            val prefixed = annotation.prefixed
            fun internalSubject(kind: String): String {
                return if (prefixed) "${client.name}.$kind.$serviceName" else "$serviceName.$kind"
            }

            val candidates = type.methods.filter { m ->
                !m.isDefault && !Modifier.isStatic(m.modifiers) && !m.isSynthetic && !m.isBridge
            }

            candidates.groupBy { it.name }
                .filterValues { it.size > 1 }
                .keys
                .forEach { problems.add("Service methods must have unique names. Found duplicate: $it") }

            fun describeMethod(method: Method, index: Int): MethodDescriptor? {
                val name = method.name
                val methodProblems = ObjectArrayList<String>()

                if (!method.isSuspend()) {
                    problems.add("$name must be a suspend function")
                    return null
                }

                val function = method.kotlinFunction
                if (function == null) {
                    problems.add("$name is not a kotlin function")
                    return null
                }

                val responders =
                    method.getAnnotation(NatsResponders::class.java)?.mode ?: serviceResponders
                val persistent =
                    method.getAnnotation(NatsPersistent::class.java)?.enabled ?: servicePersistent
                if (persistent && responders == ResponderMode.ALL) {
                    methodProblems.add("$name: @NatsPersistent is not supported with ALL responders, mark it @NatsPersistent(false)")
                }

                val methodToken =
                    method.getAnnotation(NatsMethod::class.java)?.name?.ifEmpty { null }
                        ?: Subjects.kebabCase(name)
                if ('.' in methodToken) {
                    methodProblems.add("$name: @NatsMethod name must be a single subject token, was '$methodToken'")
                }
                if (methodToken.startsWith('$')) {
                    methodProblems.add("$name: @NatsMethod name must not start with '$', was '$methodToken'")
                }

                val timeoutSeconds = method.getAnnotation(NatsTimeout::class.java)?.seconds
                if (timeoutSeconds != null && timeoutSeconds <= 0) {
                    methodProblems.add("$name: @NatsTimeout must be positive, was $timeoutSeconds")
                }
                val timeout = timeoutSeconds?.takeIf { it > 0 }?.seconds
                    ?: serviceTimeout
                    ?: if (persistent) RpcDefaults.JETSTREAM_TIMEOUT else RpcDefaults.CORE_TIMEOUT
                if (persistent && timeout > RpcDefaults.DUPLICATE_WINDOW) {
                    methodProblems.add("$name: timeout $timeout of a persistent method exceeds the JetStream duplicate window of ${RpcDefaults.DUPLICATE_WINDOW}")
                }

                val format = method.getAnnotation(NatsFormat::class.java)?.format
                    ?: serviceFormat
                    ?: WireFormat.CBOR

                val parameters = function.parameters.filter { it.kind == KParameter.Kind.VALUE }
                val argumentNames = parameters.map { param ->
                    val explicit = param.findAnnotation<NatsParam>()?.name
                    if (explicit != null && explicit.isBlank()) {
                        methodProblems.add("$name: @NatsParam on parameter '${param.name}' must not be blank")
                    }
                    explicit?.takeIf { it.isNotBlank() }
                        ?: requireNotNull(param.name) { "$name has an unnamed parameter" }
                }
                argumentNames.groupBy { it }
                    .filterValues { it.size > 1 }
                    .keys
                    .forEach { methodProblems.add("$name: argument name '$it' is used by more than one parameter") }

                val serializers = parameters.mapNotNull { param ->
                    if (param.isOptional) {
                        client.logger.warn(
                            "${type.name}.$name: parameter '${param.name}' has a default value. " +
                                    "Defaults are evaluated by the caller, so old callers keep sending the old default."
                        )
                    }

                    try {
                        serializersFor(param.type, codec)
                    } catch (e: Exception) {
                        methodProblems.add("$name: parameter '${param.name}' (${param.type}) has no serializer: ${e.message}")
                        null
                    }
                }

                val result = try {
                    resultKind(function.returnType, codec)
                } catch (e: Exception) {
                    methodProblems.add("$name: return type (${function.returnType}) is not serializable: ${e.message}")
                    null
                }
                if (responders == ResponderMode.ALL && result is ResultKind.Value) {
                    methodProblems.add("$name uses ALL responders and must return either Answer<T> or Unit")
                }

                val subject = try {
                    val raw = "${internalSubject(if (persistent) PERSIST else RPC)}.$methodToken"

                    Subjects.requireValidSubject(raw, "${type.name}.$name")
                } catch (e: IllegalArgumentException) {
                    methodProblems.add(e.message.orEmpty())
                    null
                }

                if (methodProblems.isNotEmpty() || result == null || subject == null) {
                    problems.addAll(methodProblems)
                    return null
                }

                val arguments = WireSerializers.of { format ->
                    ArgumentSerializer(
                        serialName = "$serviceName.$name",
                        names = argumentNames,
                        serializer = serializers.map { it[format] },
                    )
                }

                return MethodDescriptor(
                    method,
                    index,
                    subject,
                    responders,
                    timeout,
                    arguments,
                    result,
                    persistent,
                    format,
                )
            }

            val methods = candidates
                .filter { m -> candidates.count { it.name == m.name } == 1 }
                .sortedBy { it.name }
                .mapIndexedNotNull { index, method -> describeMethod(method, index) }

            methods.groupBy { it.subject }
                .filterValues { it.size > 1 }
                .forEach { (subject, clashing) ->
                    problems.add(
                        "Service methods must have unique subjects. '$subject' is used by " +
                                clashing.joinToString { it.method.name }
                    )
                }

            require(problems.isEmpty) {
                "Invalid @NatsService ${type.name}:\n" + problems.joinToString("\n") { " - $it" }
            }

            require(methods.isNotEmpty()) { "${type.name} has no service methods" }

            val presenceSubject = if (methods.any { it.responders == ResponderMode.ALL }) {
                Subjects.requireValidSubject(internalSubject(PRESENCE), "${type.name} presence")
            } else {
                null
            }

            val streams = if (methods.any { it.persistent }) {
                val stream =
                    (if (prefixed) "SURF_${client.name}_$serviceName" else "SURF__$serviceName")
                        .uppercase()
                        .replace("-", "_")
                PersistentStreams(
                    stream = stream,
                    filter = "${internalSubject(PERSIST)}.*",
                    dlqStream = "${stream}_DLQ",
                    dlqSubject = internalSubject(DLQ),
                )
            } else {
                null
            }

            val responderGroup = if (prefixed) client.name else serviceName
            return ServiceDescriptor(
                type,
                serviceName,
                methods,
                presenceSubject,
                streams,
                responderGroup
            )
        }

        private fun notAnInterfaceMessage(type: Class<*>): String {
            val services = ObjectLinkedOpenHashSet<Class<*>>()
            fun collect(current: Class<*>) {
                for (candidate in current.interfaces) {
                    if (candidate.isAnnotationPresent(NatsService::class.java)) services.add(
                        candidate
                    )
                    collect(candidate)
                }
            }
            generateSequence(type) { it.superclass }.forEach(::collect)

            if (services.isEmpty()) return "${type.name} must be an interface"
            return "${type.name} must be an interface. Use the @NatsService interface it implements as the " +
                    "type argument, e.g. registerService<${services.first().simpleName}>(implementation): " +
                    services.joinToString { it.name }
        }

        private fun resultKind(returnType: KType, codec: NatsCodec): ResultKind {
            return when (returnType.classifier) {
                Unit::class -> ResultKind.Unit
                Answer::class -> {
                    val valueType = requireNotNull(returnType.arguments[0].type) {
                        "Answer<*> is not allowed, the value type must be explicit"
                    }
                    ResultKind.Answer(serializersFor(valueType, codec))
                }

                else -> ResultKind.Value(serializersFor(returnType, codec))
            }
        }

        private fun serializersFor(type: KType, codec: NatsCodec): WireSerializers<Any?> {
            return WireSerializers.of { wireFormat ->
                TypeSerializers.serializerFor(type, codec.serializersModule(wireFormat))
            }
        }
    }
}