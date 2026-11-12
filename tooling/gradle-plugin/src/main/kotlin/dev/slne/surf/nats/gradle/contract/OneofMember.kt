package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.OneOf

/**
 * The nullable property that holds the active case of a oneof.
 *
 * [cases] are in ascending field number.
 */
internal class OneofMember(
    val oneOf: OneOf,
    override val name: String,
    val interfaceName: ClassName,
    val cases: List<OneofCase>,
) : MessageMember {
    override val protoName: String get() = oneOf.name
    override val location: Location get() = oneOf.location
    override val type: TypeName = interfaceName.copy(nullable = true)
    override val default: CodeBlock = CodeBlock.of("null")
    override val comparison: MemberComparison get() = MemberComparison.EQUALS
}
