package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.protocol.ProtocolDescriptors
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import java.lang.reflect.Modifier

/**
 * Reads the static `NATS_DESCRIPTOR` field a generated event class or service interface declares itself.
 */
@InternalNatsApi
internal object GeneratedDescriptors {
    private const val FIELD = ProtocolDescriptors.FIELD

    private val NONE = Any()

    private val descriptors = object : ClassValue<Any>() {
        override fun computeValue(type: Class<*>): Any = read(type) ?: NONE
    }

    fun event(type: Class<*>): NatsEventDescriptor<*> {
        val descriptor = descriptors.get(type)
        require(descriptor is NatsEventDescriptor<*>) {
            "${type.name} is not a generated surf-nats event: it declares no NatsEventDescriptor $FIELD"
        }
        return descriptor
    }

    fun service(type: Class<*>): NatsServiceDescriptor<*> {
        val descriptor = descriptors.get(type)
        require(type.isInterface && descriptor is NatsServiceDescriptor<*>) { notAServiceMessage(type) }
        return descriptor
    }

    private fun read(type: Class<*>): Any? {
        val field = try {
            type.getField(FIELD)
        } catch (_: NoSuchFieldException) {
            return null
        }
        if (field.declaringClass !== type || !Modifier.isStatic(field.modifiers)) return null
        field.trySetAccessible()
        return field.get(null)
    }

    private fun notAServiceMessage(type: Class<*>): String {
        val services = ObjectLinkedOpenHashSet<Class<*>>()
        fun collect(current: Class<*>) {
            for (candidate in current.interfaces) {
                if (descriptors.get(candidate) is NatsServiceDescriptor<*>) services.add(candidate)
                collect(candidate)
            }
        }
        generateSequence(type) { it.superclass }.forEach(::collect)

        val problem = "${type.name} is not a generated surf-nats service interface"
        if (services.isEmpty()) return "$problem: it declares no NatsServiceDescriptor $FIELD"
        return "$problem. Use the generated service interface it implements as the type argument, " +
                "e.g. registerService<${services.first().simpleName}>(implementation): " +
                services.joinToString { it.name }
    }
}
