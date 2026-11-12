package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.descriptor.GeneratedDescriptors
import dev.slne.surf.nats.descriptor.NatsMethodDescriptor
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.protocol.ProtocolFieldRules
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.util.isSuspend
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.time.Duration.Companion.seconds

/**
 * Runtime model of a service interface: its wire names and its methods resolved against the generated descriptor.
 *
 * [methods] are in the order of the generated descriptor, and [MethodDescriptor.index] is the position in that list.
 * Caller proxies and service registrations address methods by that index. Create instances with [of].
 */
@InternalNatsApi
internal class ServiceDescriptor(
    val type: Class<*>,
    val names: ServiceNames,
    val methods: List<MethodDescriptor>,
) {
    /**
     * Proto full name of the service. A client registers at most one implementation per name.
     */
    val name: String get() = names.service

    /**
     * Subject of the presence protocol, or `null` when no method is an `ALL_INSTANCES` call with an [Answer] result.
     */
    val presenceSubject: String? = if (methods.any { it.recipients == Recipients.ALL_INSTANCES && it.result == NatsResultKind.ANSWER }) {
        names.presenceSubject
    } else {
        null
    }

    // Names of the work queue and dead-letter streams, null without persistent methods
    val streams: ServiceNames? = if (methods.any { it.persistent }) names else null

    companion object {
        /**
         * Reads the generated descriptor of [type] and resolves it against the interface methods.
         *
         * Throws [IllegalArgumentException] listing every problem when the descriptor does not match [type] or names
         * an invalid service or rpc.
         */
        fun of(type: Class<*>): ServiceDescriptor {
            val generated = GeneratedDescriptors.service(type)
            ServiceNames.problem(generated.protoName)?.let { problem ->
                throw IllegalArgumentException("Invalid NATS_DESCRIPTOR of ${type.name}: $problem")
            }
            val names = ServiceNames(generated.protoName)

            // Static, synthetic, bridge and default members are not rpc methods
            val candidates = type.methods.filter { m ->
                !Modifier.isStatic(m.modifiers) && !m.isSynthetic && !m.isBridge && !m.isDefault
            }

            val problems = ArrayList<String>()
            val resolved = HashSet<Method>()
            val methods = ArrayList<MethodDescriptor>(generated.methods.size)

            generated.methods.forEachIndexed { index, method ->
                val label = "${method.kotlinName} (rpc ${method.protoName})"
                val jvmMethod = resolve(candidates, method, label, problems) ?: return@forEachIndexed
                if (!resolved.add(jvmMethod)) {
                    problems.add("$label: ${jvmMethod.name} is described by more than one rpc")
                    return@forEachIndexed
                }

                if ((method.responseSerializer == null) != (method.resultKind == NatsResultKind.UNIT)) {
                    problems.add("$label: a response serializer is required exactly for non-UNIT results")
                    return@forEachIndexed
                }

                ServiceNames.methodProblem(names.service, method.protoName)?.let { problem ->
                    problems.add("$label: $problem")
                    return@forEachIndexed
                }
                val subject = names.methodSubject(method.protoName, method.persistent)

                val timeout = ProtocolResolution
                    .effectiveCallTimeoutSeconds(method.callTimeoutSeconds, method.persistent)
                    .seconds

                @Suppress("UNCHECKED_CAST")
                methods.add(
                    MethodDescriptor(
                        method as NatsMethodDescriptor<Any, *>,
                        jvmMethod,
                        index,
                        subject,
                        timeout,
                        "${type.name}.${method.kotlinName}",
                    )
                )
            }

            candidates.filter { Modifier.isAbstract(it.modifiers) && it !in resolved }
                .forEach { problems.add("${it.name} is not described by any rpc of the descriptor") }

            require(problems.isEmpty()) {
                "Invalid NATS_DESCRIPTOR of ${type.name}:\n" + problems.joinToString("\n") { " - $it" }
            }

            require(methods.isNotEmpty()) { "${type.name} has no service methods" }

            return ServiceDescriptor(type, names, methods)
        }

        // The suspend method kotlinName(request: <message>), or kotlinName() for a google.protobuf.Empty request
        private fun resolve(
            candidates: List<Method>,
            method: NatsMethodDescriptor<*, *>,
            label: String,
            problems: MutableList<String>,
        ): Method? {
            val emptyRequest = method.requestSerializer.descriptor.serialName == ProtocolFieldRules.EMPTY_TYPE
            val parameterCount = if (emptyRequest) 1 else 2
            val named = candidates.filter { m -> m.name == method.kotlinName && m.isSuspend() }
            val matches = named.filter { m -> m.parameterCount == parameterCount }

            val expected = if (emptyRequest) {
                "${method.kotlinName}() for the ${ProtocolFieldRules.EMPTY_TYPE} request"
            } else {
                "${method.kotlinName}(request: ${method.requestSerializer.descriptor.serialName})"
            }

            return when (matches.size) {
                1 -> {
                    val match = matches.single()
                    if (!emptyRequest && match.parameterTypes[0].isPrimitive) {
                        problems.add("$label: expected $expected, found the primitive parameter ${match.parameterTypes[0]}")
                        null
                    } else {
                        match
                    }
                }

                0 -> {
                    val found = if (named.isEmpty()) "" else ", found " + named.joinToString { it.signature() }
                    problems.add("$label: no suspend method $expected$found")
                    null
                }

                else -> {
                    problems.add("$label: matches more than one method: ${matches.joinToString { it.signature() }}")
                    null
                }
            }
        }

        // name(parameter types without the Continuation)
        private fun Method.signature(): String {
            return parameterTypes.dropLast(1).joinToString(prefix = "$name(", postfix = ")") { it.typeName }
        }
    }
}
