package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility

/**
 * Applies `generatedVisibility` to a class, interface or enum declaration. Members are emitted `public`.
 */
internal fun TypeSpec.Builder.applyVisibility(
    visibility: GeneratedVisibility,
): TypeSpec.Builder = apply {
    when (visibility) {
        GeneratedVisibility.PUBLIC -> addModifiers(KModifier.PUBLIC)
        GeneratedVisibility.INTERNAL -> addModifiers(KModifier.INTERNAL)
    }
}
