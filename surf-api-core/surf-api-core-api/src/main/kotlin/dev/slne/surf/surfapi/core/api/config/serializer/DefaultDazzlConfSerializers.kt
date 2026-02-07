package dev.slne.surf.surfapi.core.api.config.serializer

import dev.slne.surf.surfapi.core.api.config.manager.PreferUsingSpongeConfigOverDazzlConf
import dev.slne.surf.surfapi.core.api.minimessage.SurfMiniMessageHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.ParsingException
import space.arim.dazzleconf.serialiser.Decomposer
import space.arim.dazzleconf.serialiser.FlexibleType
import space.arim.dazzleconf.serialiser.ValueSerialiser
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Default serializers for DazzlConf configuration files. Provides support for custom types such as Adventure [Component].
 */
@PreferUsingSpongeConfigOverDazzlConf
object DefaultDazzlConfSerializers {

    /**
     * The default list of serializers used in DazzlConf configurations.
     */
    val DEFAULTS = CopyOnWriteArrayList<ValueSerialiser<*>>()

    init {
        DEFAULTS.add(ComponentSerializer())
    }

    /**
     * Serializer for [Component] objects in DazzlConf configurations.
     */
    class ComponentSerializer : ValueSerialiser<Component> {
        override fun getTargetClass() = Component::class.java
        override fun deserialise(flexibleType: FlexibleType): Component {
            try {
                return SurfMiniMessageHolder.miniMessage().deserialize(flexibleType.string)
            } catch (e: ParsingException) {
                throw flexibleType.badValueExceptionBuilder()
                    .message(
                        """
                        Failed to parse component from string.
                        Caused by: ${e.detailMessage()}
                        at: ${e.endIndex()}
                        """.trimIndent()
                    )
                    .cause(e)
                    .build()
            }
        }

        override fun serialise(value: Component, decomposer: Decomposer?) =
            SurfMiniMessageHolder.miniMessage().serialize(value)

        companion object {
            @JvmStatic
            @Deprecated(
                message = "Configs now use the MiniMessage instance supplied by the SurfMiniMessageHolder",
                replaceWith = ReplaceWith(
                    "SurfMiniMessageHolder.miniMessage()",
                    "dev.slne.surf.surfapi.core.api.minimessage.SurfMiniMessageHolder"
                )
            )
            val miniMessage: MiniMessage = SurfMiniMessageHolder.miniMessage()

            @Deprecated(
                message = "Cannot customize MiniMessage anymore. If you need custom tags, use your own MiniMessage instance instead and use a String in the config, then parse it manually (Consider caching the parsed value in a lazy value though).",
                level = DeprecationLevel.ERROR
            )
            fun customizeMiniMessage(modifier: (MiniMessage.Builder) -> Unit) {
            }
        }
    }
}
