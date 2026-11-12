package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.wire.schema.Location

/**
 * A constructor property of a generated message or oneof case class.
 */
internal sealed interface MessageMember {
    val protoName: String
    val location: Location
    val name: String
    val type: TypeName
    val default: CodeBlock?
    val comparison: MemberComparison
}
