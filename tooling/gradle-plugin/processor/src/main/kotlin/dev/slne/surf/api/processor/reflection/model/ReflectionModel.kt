package dev.slne.surf.api.processor.reflection.model

import com.google.devtools.ksp.symbol.KSClassDeclaration

data class ReflectionModel(
    val packageName: String,
    val className: String,
    val interfaceName: String,
    val singleton: Boolean,
    val members: List<ReflectedMember>,
    val source: KSClassDeclaration,
)
