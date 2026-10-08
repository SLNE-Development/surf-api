package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.jetstream.PersistentStreams
import dev.slne.surf.nats.subject.Subjects
import dev.slne.surf.nats.util.isSuspend
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.serialization.modules.SerializersModule
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.Duration.Companion.seconds

@InternalNatsApi
internal class ServiceDescriptor(
    val type: Class<*>,
    val name: String,
    val methods: List<MethodDescriptor>,
    val presenceSubject: String?,
    val streams: PersistentStreams?,
) {
    companion object {
        fun of(type: Class<*>, client: NatsClientImpl): ServiceDescriptor {
            require(type.isInterface) { "${type.name} must be an interface" }
            val annotation = requireNotNull(type.getAnnotation(NatsService::class.java)) {
                "${type.name} must be annotated with @NatsService"
            }
            val servicePersistent = type.getAnnotation(Persistent::class.java)?.enabled ?: false
            val serviceTimeout = type.getAnnotation(NatsTimeout::class.java)?.seconds?.seconds

            val codec = client.codec
            val serviceName = annotation.name.ifEmpty { Subjects.kebabCase(type.simpleName) }
            val module = codec.serializersModule(WireFormat.CBOR)

            val problems = ObjectArrayList<String>()
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
                    method.getAnnotation(Responders::class.java)?.mode ?: annotation.responders
                val persistent =
                    method.getAnnotation(Persistent::class.java)?.enabled ?: servicePersistent
                if (persistent && responders == ResponderMode.ALL) {
                    methodProblems.add("$name: @Persistent is not supported with ALL responders, mark it @Persistent(false)")
                }

                val timeout = method.getAnnotation(NatsTimeout::class.java)?.seconds?.seconds
                    ?: serviceTimeout
                    ?: if (persistent) RpcDefaults.JETSTREAM_TIMEOUT else RpcDefaults.CORE_TIMEOUT

                val parameters = function.parameters.filter { it.kind == KParameter.Kind.VALUE }
                val serializers = parameters.mapNotNull { param ->
                    if (param.isOptional) {
                        client.logger.warn(
                            "${type.name}.$name: parameter '${param.name}' has a default value. " +
                                    "Defaults are evaluated by the caller, so old callers keep sending the old default."
                        )
                    }

                    try {
                        TypeSerializers.serializerFor(param.type, module)
                    } catch (e: Exception) {
                        methodProblems.add("$name: parameter '${param.name}' (${param.type}) has no serializer: ${e.message}")
                        null
                    }
                }

                val result = try {
                    resultKind(function.returnType, module)
                } catch (e: Exception) {
                    methodProblems.add("$name: return type (${function.returnType}) is not serializable: ${e.message}")
                    null
                }
                if (responders == ResponderMode.ALL && result is ResultKind.Value) {
                    methodProblems.add("$name uses ALL responders and must return either Answer<T> or Unit")
                }

                val subject = try {
                    val raw = if (persistent) {
                        $$"$${client.name}.$persist.$$serviceName.$${Subjects.kebabCase(name)}"
                    } else {
                        "${client.name}.$serviceName.${Subjects.kebabCase(name)}"
                    }

                    Subjects.requireValidSubject(raw, "${type.name}.$name")
                } catch (e: IllegalArgumentException) {
                    methodProblems.add(e.message.orEmpty())
                    null
                }

                if (methodProblems.isNotEmpty() || result == null || subject == null) {
                    problems.addAll(methodProblems)
                    return null
                }

                val arguments = ArgumentSerializer(
                    serialName = "$serviceName.$name",
                    names = parameters.map { requireNotNull(it.name) { "$name has an unnamed parameter" } },
                    serializer = serializers,
                )

                return MethodDescriptor(
                    method,
                    index,
                    subject,
                    responders,
                    timeout,
                    arguments,
                    result,
                    persistent,
                )
            }

            val methods = candidates
                .filter { m -> candidates.count { it.name == m.name } == 1 }
                .sortedBy { it.name }
                .mapIndexedNotNull { index, method -> describeMethod(method, index) }

            require(problems.isEmpty) {
                "Invalid @NatsService ${type.name}:\n" + problems.joinToString("\n") { " - $it" }
            }

            require(methods.isNotEmpty()) { "${type.name} has no service methods" }

            val presenceSubject = if (methods.any { it.responders == ResponderMode.ALL }) {
                Subjects.requireValidSubject(
                    $$"$${client.name}.$presence.$$serviceName",
                    "${type.name} presence",
                )
            } else {
                null
            }

            val streams = if (methods.any { it.persistent }) {
                val stream = "SURF_${client.name}_$serviceName".uppercase().replace("-", "_")
                PersistentStreams(
                    stream = stream,
                    filter = $$"$${client.name}.$persist.$$serviceName.>",
                    dlqStream = "${stream}_DLQ",
                    dlqSubject = $$"$${client.name}.$dlq.$$serviceName",
                )
            } else {
                null
            }

            return ServiceDescriptor(type, serviceName, methods, presenceSubject, streams)
        }

        private fun resultKind(returnType: KType, module: SerializersModule): ResultKind {
            return when (returnType.classifier) {
                Unit::class -> ResultKind.Unit
                Answer::class -> {
                    val valueType = requireNotNull(returnType.arguments[0].type) {
                        "Answer<*> is not allowed, the value type must be explicit"
                    }
                    ResultKind.Answer(TypeSerializers.serializerFor(valueType, module))
                }

                else -> ResultKind.Value(TypeSerializers.serializerFor(returnType, module))
            }
        }
    }
}