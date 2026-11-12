package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.ClassName

/**
 * A case of a oneof: the data class [className] that wraps the case field as [value].
 */
internal class OneofCase(val className: ClassName, val value: FieldMember)
