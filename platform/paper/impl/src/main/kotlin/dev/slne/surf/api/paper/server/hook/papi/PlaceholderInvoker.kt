package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.common.invoker.InvokerFactory
import dev.slne.surf.api.common.invoker.InvokerMethods
import dev.slne.surf.api.paper.hook.papi.Placeholder
import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.springframework.core.KotlinDetector
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.kotlinFunction

/**
 * Calls one [Placeholder] method.
 */
internal fun interface PlaceholderInvoker {

    /**
     * Calls the method.
     *
     * @param player the requesting player, or `null` for a request without one
     * @param args the request's arguments
     * @return the method's `String` or `Component` result, or `null`
     */
    fun invoke(player: OfflinePlayer?, args: String): Any?
}

/**
 * Creates the hidden-class invokers of [Placeholder] methods.
 *
 * A method whose player parameter is `Player` returns `null` without being called for a request
 * whose player is not online, and a Kotlin method whose player parameter is a non-null
 * `OfflinePlayer` does so for a request without a player. A method without an arguments parameter
 * ignores the arguments.
 */
internal object PlaceholderInvokers {
    private val lookup = MethodHandles.lookup()
    private val factory = InvokerFactory(PlaceholderInvoker::class.java, lookup)

    private val isPlayer = lookup.findVirtual(
        Class::class.java,
        "isInstance",
        MethodType.methodType(Boolean::class.javaPrimitiveType, Any::class.java),
    ).bindTo(Player::class.java)

    private val isNotNull = lookup.findStatic(
        Objects::class.java,
        "nonNull",
        MethodType.methodType(Boolean::class.javaPrimitiveType, Any::class.java),
    )

    /**
     * Creates an invoker of [method] on [target].
     *
     * @param target the instance declaring [method]
     * @param method the [Placeholder] method
     * @return the invoker
     * @throws IllegalArgumentException if [method] does not have a placeholder method's signature
     * or cannot be accessed
     */
    fun create(target: Any, method: Method): PlaceholderInvoker {
        requireSignature(method)
        require(InvokerMethods.canAccess(method, lookup)) { "$method cannot be accessed" }

        val type = factory.invokerMethodType
        val bound = InvokerMethods.bind(method, target, lookup)
        val withArgs = if (method.parameterCount == 2) {
            bound
        } else {
            MethodHandles.dropArguments(bound, 1, String::class.java)
        }
        val call = withArgs.asType(type)
        val guardedCall = when {
            method.parameterTypes[0] == Player::class.java -> guarded(call, isPlayer)
            !acceptsMissingPlayer(method) -> guarded(call, isNotNull)
            else -> call
        }

        return factory.create(guardedCall)
    }

    private fun guarded(call: MethodHandle, playerTest: MethodHandle): MethodHandle {
        val test = playerTest.asType(
            MethodType.methodType(Boolean::class.javaPrimitiveType, OfflinePlayer::class.java),
        )

        return MethodHandles.guardWithTest(test, call, MethodHandles.empty(call.type()))
    }

    private fun acceptsMissingPlayer(method: Method): Boolean {
        if (!KotlinDetector.isKotlinType(method.declaringClass)) return true

        val playerParameter = method.kotlinFunction?.valueParameters?.firstOrNull() ?: return true

        return playerParameter.type.isMarkedNullable
    }

    private fun requireSignature(method: Method) {
        require(!InvokerMethods.isSuspendFunction(method)) { "$method must not be suspend" }

        val parameters = method.parameterTypes
        require(parameters.size in 1..2) { "$method must take a player and optionally a String" }

        val playerType = parameters[0]
        require(playerType == OfflinePlayer::class.java || playerType == Player::class.java) {
            "The first parameter of $method must be OfflinePlayer or Player"
        }
        require(parameters.size == 1 || parameters[1] == String::class.java) {
            "The second parameter of $method must be a String"
        }

        val returnType = method.returnType
        val isText = returnType == String::class.java
        require(isText || Component::class.java.isAssignableFrom(returnType)) {
            "$method must return String or Component"
        }
    }
}
