package dev.slne.surf.surfapi.core.api.config.serializer

import dev.slne.surf.surfapi.core.api.config.PreferUsingSpongeConfigOverDazzlConf
import dev.slne.surf.surfapi.core.api.messages.Colors
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.ParsingException
import net.kyori.adventure.text.minimessage.tag.Tag
import space.arim.dazzleconf.serialiser.Decomposer
import space.arim.dazzleconf.serialiser.FlexibleType
import space.arim.dazzleconf.serialiser.ValueSerialiser

/**
 * These are the default serializers that are always enabled when creating a configuration via
 * [DazzlConfConfigManager.create]
 */
@PreferUsingSpongeConfigOverDazzlConf
object DefaultDazzlConfSerializers {
    val DEFAULTS = mutableListOf<ValueSerialiser<*>>(ComponentSerializer())

    class ComponentSerializer : ValueSerialiser<Component> {
        override fun getTargetClass() = Component::class.java
        override fun deserialise(flexibleType: FlexibleType): Component {
            try {
                return miniMessage.deserialize(flexibleType.string)
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
            miniMessage.serialize(value)

        companion object {
            private val builder = MiniMessage.builder()
                .editTags {
                    val tags = mapOf(
                        "primary" to Colors.PRIMARY,
                        "secondary" to Colors.SECONDARY,
                        "info" to Colors.INFO,
                        "success" to Colors.SUCCESS,
                        "warning" to Colors.WARNING,
                        "error" to Colors.ERROR,
                        "variable_key" to Colors.VARIABLE_KEY,
                        "variable_value" to Colors.VARIABLE_VALUE,
                        "spacer" to Colors.SPACER,
                        "dark_spacer" to Colors.DARK_SPACER,
                        "prefix_color" to Colors.PREFIX_COLOR
                    )

                    tags.forEach { (tag, color) ->
                        it.tag(tag) { _, _ -> colorTag(color) }
                    }
                }

            @JvmStatic
            var miniMessage: MiniMessage = builder.build()
                get() {
                    if (modified) {
                        field = builder.build()
                        modified = false
                    }

                    return field
                }
                private set
            private var modified = false

            private fun colorTag(color: TextColor) = Tag.styling { it.color(color) }

            fun customizeMiniMessage(modifier: (MiniMessage.Builder) -> Unit) {
                modifier(builder)
                modified = true
            }
        }
    }
}
