package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.bytecode.ValueClassParameters
import dev.slne.surf.nats.descriptor.GeneratedDescriptors
import dev.slne.surf.nats.descriptor.NatsMethodDescriptor
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.protocol.ProtocolResolution
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.util.isSuspend
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.time.Duration.Companion.seconds

@InternalNatsApi
internal class ServiceDescriptor(
    val type: Class<*>,
    val names: ServiceNames,
    val methods: List<MethodDescriptor>,
) {
    /**
     * Proto full name of the service; a client registers at most one implementation per name.
     */
    val name: String get() = names.service

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

            // Static and synthetic members include the `$default` bridges of default parameter values
            val candidates = type.methods.filter { m ->
                !Modifier.isStatic(m.modifiers) && !m.isSynthetic && !m.isBridge && !m.isDefault
            }

            val problems = ObjectArrayList<String>()
            val resolved = ObjectOpenHashSet<Method>()
            val methods = ObjectArrayList<MethodDescriptor>(generated.methods.size)

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

                val valueClasses = ValueClassParameters.of(jvmMethod, method.parameters, label, problems)
                    ?: return@forEachIndexed

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
                        valueClasses,
                        "${type.name}.${method.kotlinName}",
                    )
                )
            }

            candidates.filter { Modifier.isAbstract(it.modifiers) && it !in resolved }
                .forEach { problems.add("${it.name} is not described by any rpc of the descriptor") }

            require(problems.isEmpty) {
                "Invalid NATS_DESCRIPTOR of ${type.name}:\n" + problems.joinToString("\n") { " - $it" }
            }

            require(methods.isNotEmpty()) { "${type.name} has no service methods" }

            return ServiceDescriptor(type, names, methods)
        }

        // kotlinName, or kotlinName-<hash> when value class parameters mangle the JVM name; Continuation last
        private fun resolve(
            candidates: List<Method>,
            method: NatsMethodDescriptor<*, *>,
            label: String,
            problems: MutableList<String>,
        ): Method? {
            val parameterCount = method.parameters.size + 1
            val mangledPrefix = method.kotlinName + "-"
            val matches = candidates.filter { m ->
                (m.name == method.kotlinName || m.name.startsWith(mangledPrefix)) &&
                        m.parameterCount == parameterCount &&
                        m.isSuspend()
            }

            return when (matches.size) {
                1 -> matches.single()
                0 -> {
                    problems.add("$label: no suspend method with ${method.parameters.size} parameters")
                    null
                }

                else -> {
                    problems.add("$label: matches more than one method: ${matches.joinToString { it.name }}")
                    null
                }
            }
        }
    }
}
