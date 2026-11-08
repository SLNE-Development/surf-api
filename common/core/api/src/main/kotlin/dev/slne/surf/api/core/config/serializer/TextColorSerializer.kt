package dev.slne.surf.api.core.config.serializer

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate

internal object TextColorSerializer : ScalarSerializer.Annotated<TextColor>(TextColor::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): TextColor {
        val input = obj.toString().trim()

        NamedTextColor.NAMES.value(input)?.let { return it }
        TextColor.fromHexString(input)?.let { return it }

        val rgb = input.split(',', ';').map { it.trim() }
        if (rgb.size == 3) {
            val red = rgb[0].toIntOrNull()
            val green = rgb[1].toIntOrNull()
            val blue = rgb[2].toIntOrNull()

            if (red != null && green != null && blue != null) {
                return TextColor.color(red, green, blue)
            }
        }

        throw SerializationException(TextColor::class.java, "$obj($type) is not a valid text color")
    }

    override fun serialize(type: AnnotatedType, item: TextColor, typeSupported: Predicate<Class<*>>): Any {
        return NamedTextColor.namedColor(item.value())?.toString() ?: item.asHexString()
    }
}