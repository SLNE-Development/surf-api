package dev.slne.surf.nats.gradle.model

import com.squareup.wire.schema.Field
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.OneOf

/**
 * Maps the fields and oneofs of [type] in Kotlin constructor order: ascending field number, a oneof at its lowest case
 * number ([Int.MAX_VALUE] without cases), fields before oneofs on a tie.
 *
 * Members are mapped in that order, so problems reported by [field] and [oneof] follow it as well.
 */
internal fun <T> orderedMembers(type: MessageType, field: (Field) -> T, oneof: (OneOf, number: Int) -> T): List<T> {
    val fields = type.declaredFields.map { it.tag to it }
    val oneofs = type.oneOfs.map { oneOf -> (oneOf.fields.minOfOrNull { it.tag } ?: Int.MAX_VALUE) to oneOf }
    return (fields + oneofs).sortedBy { it.first }.map { (number, member) ->
        if (member is Field) field(member) else oneof(member as OneOf, number)
    }
}
