package dev.slne.surf.api.processor.util

import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

fun KSClassDeclaration.toClassName(): ClassName {
    require(!isLocal()) { "Local/anonymous classes are not supported!" }
    val pkg = packageName.asString()
    val typesString = qualifiedName!!.asString().removePrefix("$pkg.")
    val simpleNames = typesString.split(".")
    return ClassName(pkg, simpleNames)
}

fun KSClassDeclaration.toBinaryName(): String = toClassName().reflectionName()

inline fun <reified T> nameOf(): String = T::class.java.name