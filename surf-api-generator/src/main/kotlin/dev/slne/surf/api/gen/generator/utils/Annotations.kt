package dev.slne.surf.api.gen.generator.utils

import com.squareup.javapoet.AnnotationSpec

object Annotations {

    private val suppressWarnings = AnnotationSpec.builder(SuppressWarnings::class.java)
        .addMember("value", "\$S", "unused")
        .addMember("value", "\$S", "SpellCheckingInspection")
        .build()

    val classHeader = listOf(suppressWarnings)
}