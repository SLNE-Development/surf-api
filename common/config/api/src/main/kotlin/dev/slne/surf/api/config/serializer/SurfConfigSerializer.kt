package dev.slne.surf.api.config.serializer

import dev.slne.surf.api.config.SurfConfig
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

/**
 * Marks a Configurate `TypeSerializer` as a bean that serializes one type in config files.
 *
 * The annotated class implements `TypeSerializer<T>` or `ScalarSerializer<T>` and becomes a bean of
 * the context that scans it, so it may inject other beans. surf-api never creates it itself.
 *
 * ```kotlin
 * @SurfConfigSerializer
 * class PriceSerializer : ScalarSerializer<Price>(Price::class.java) {
 *     ...
 * }
 * ```
 *
 * The serializer applies to the type argument of its `TypeSerializer` supertype, generics
 * included, so `TypeSerializer<List<Price>>` serializes `List<Price>`; a `ScalarSerializer` applies
 * to its `type()`. Like every Configurate registration, it also serializes subtypes of that type.
 *
 * Each config file asks three levels for a serializer, the first match winning:
 * 1. the serializers its [SurfConfig] names in [SurfConfig.serializers]
 * 2. the [applyToAll] serializers of the plugin that declares the config
 * 3. the [applyToAll] serializers of surf-api's root context, which cover surf-api's own types
 *
 * Two serializers of one level for the same type fail the plugin's start. A plugin's serializers
 * never apply to the configs of another plugin.
 *
 * @property applyToAll whether the serializer applies to every config of its context: to every
 * config of the plugin, or, in surf-api's root context, to every config of every plugin and to
 * files opened through `ConfigHandles.open`. `false` limits it to configs that name it in
 * [SurfConfig.serializers].
 * @property type the type to serialize, for a serializer whose type argument cannot be resolved,
 * such as one declared with a type variable. [Nothing] resolves the type from the class.
 */
@Component
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SurfConfigSerializer(
    val applyToAll: Boolean = true,
    val type: KClass<*> = Nothing::class,
)
