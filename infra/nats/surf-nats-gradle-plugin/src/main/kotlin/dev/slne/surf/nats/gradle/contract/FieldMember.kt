package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import dev.slne.surf.nats.gradle.model.IntEncoding

/**
 * The property of a field; for a oneof case, the `value` property of its case class.
 *
 * [encoding] is the integer encoding of the element, for a map the non-default one of key and value. `null` and
 * [IntEncoding.DEFAULT] need no annotation.
 */
internal class FieldMember(
    val field: Field,
    override val name: String,
    override val type: TypeName,
    override val default: CodeBlock?,
    override val comparison: MemberComparison,
    val encoding: IntEncoding?,
    val packed: Boolean,
) : MessageMember {
    override val protoName: String get() = this.field.name
    override val location: Location get() = this.field.location
}
