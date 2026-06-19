package dev.slne.surf.api.core.messages.adventure

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.event.ClickEvent
import java.util.function.Consumer
import kotlin.experimental.ExperimentalTypeInference
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun <C : Component, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickCallback(
    callback: ClickCallback<Audience>,
) = clickEvent(ClickEvent.callback(callback))

fun <C : Component, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickCallbackWithOptions(
    builder: ClickCallbackWithOptionsBuilder<Audience>.() -> Unit,
) = clickEvent(ClickCallbackWithOptionsBuilder(Audience::class.java).apply(builder).build())

inline fun <reified T : Audience, C : Component, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickCallbackTyped(
    callback: ClickCallback<T>,
) = clickEvent(ClickEvent.callback(ClickCallback.widen(callback, T::class.java)))

@OptIn(ExperimentalTypeInference::class)
inline fun <reified T : Audience, C : Component, B : ComponentBuilder<C, B>> ComponentBuilder<C, B>.clickCallbackTypedWithOptions(
    @BuilderInference builder: ClickCallbackWithOptionsBuilder<T>.() -> Unit
) = clickEvent(ClickCallbackWithOptionsBuilder(T::class.java).apply(builder).build())

fun ClickCallback.Options.Builder.lifetime(duration: Duration) = lifetime(duration.toJavaDuration())

@DslMarker
annotation class ClickCallbackWithOptionsBuilderDsl

@ClickCallbackWithOptionsBuilderDsl
class ClickCallbackWithOptionsBuilder<T : Audience> @PublishedApi internal constructor(private val type: Class<T>) {
    private var callback: ClickCallback<T>? = null
    private var permission: String? = null
    private var permissionOtherwise: Consumer<in Audience>? = null
    private var options: ClickCallback.Options = ClickCallback.Options.builder().build()

    fun requiresPermission(permission: String) {
        this.permission = permission
    }

    fun requiresPermissionOrElse(
        permission: String,
        callback: Consumer<in Audience>
    ) {
        this.permission = permission
        this.permissionOtherwise = callback
    }

    fun callback(callback: ClickCallback<T>) {
        this.callback = callback
    }

    fun options(options: ClickCallback.Options.Builder.() -> Unit) {
        this.options = ClickCallback.Options.builder(this.options).apply(options).build()
    }

    @PublishedApi
    internal fun build(): ClickEvent<*> {
        val callback = callback ?: return ClickEvent.callback { }
        val callbackWithPermission = permission
            ?.let { permission -> callback.requiringPermission(permission, permissionOtherwise) }
            ?: callback

        return ClickEvent.callback(ClickCallback.widen(callbackWithPermission, type), options)
    }
}
