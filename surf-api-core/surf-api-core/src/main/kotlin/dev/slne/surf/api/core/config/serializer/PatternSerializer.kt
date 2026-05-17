package dev.slne.surf.api.core.config.serializer

import org.spongepowered.configurate.serialize.ScalarSerializer
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.AnnotatedType
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

internal object PatternSerializer : ScalarSerializer.Annotated<Pattern>(Pattern::class.java) {
    override fun deserialize(type: AnnotatedType, obj: Any): Pattern {
        return try {
            Pattern.compile(obj.toString())
        } catch (e: PatternSyntaxException) {
            throw SerializationException(Pattern::class.java, "$obj($type) is not a valid pattern", e)
        }
    }

    override fun serialize(type: AnnotatedType, item: Pattern, typeSupported: Predicate<Class<*>>): Any {
        return item.pattern()
    }
}