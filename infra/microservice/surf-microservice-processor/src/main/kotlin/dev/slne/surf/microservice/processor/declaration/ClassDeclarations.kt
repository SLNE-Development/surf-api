package dev.slne.surf.microservice.processor.declaration

import com.google.devtools.ksp.findActualType
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.Modifier

/**
 * Returns the top-level and nested classes of this file.
 *
 * Local and anonymous classes are not part of the result because they are declared inside function bodies.
 */
internal fun KSFile.classDeclarations(): Sequence<KSClassDeclaration> =
    declarations.filterIsInstance<KSClassDeclaration>().flatMap { it.withNestedClasses() }

private fun KSClassDeclaration.withNestedClasses(): Sequence<KSClassDeclaration> =
    sequenceOf(this) + declarations.filterIsInstance<KSClassDeclaration>().flatMap { it.withNestedClasses() }

/**
 * Whether this is a class or object that can have instances of its own, so not abstract, sealed, an interface, an
 * enum or an annotation.
 */
internal fun KSClassDeclaration.isConcrete(): Boolean =
    classKind in CONCRETE_KINDS && !isAbstract() && Modifier.SEALED !in modifiers

private val CONCRETE_KINDS = setOf(ClassKind.CLASS, ClassKind.OBJECT)

/**
 * Returns the JVM binary name of this class, for example `com.example.Outer$Inner`.
 */
internal fun KSClassDeclaration.jvmBinaryName(): String =
    jvmBinaryName(packageName.asString(), enclosingClassesAndSelf().map { it.simpleName.asString() }.toList().asReversed())

/**
 * Returns this declaration followed by its enclosing classes, innermost first.
 */
internal fun KSDeclaration.enclosingClassesAndSelf(): Sequence<KSDeclaration> =
    generateSequence(this) { it.parentDeclaration }

/**
 * Returns the qualified names of all direct and indirect supertypes, with type aliases expanded.
 *
 * Returns `null` while a supertype cannot be resolved, for example because another processor generates it in a later
 * round.
 */
internal fun KSClassDeclaration.supertypeNamesOrNull(): Set<String>? {
    val supertypes = getAllSuperTypes().toList()
    if (supertypes.any { it.isError }) return null

    return supertypes.mapNotNullTo(HashSet()) { it.declaration.expandTypeAlias().qualifiedName?.asString() }
}

private fun KSDeclaration.expandTypeAlias(): KSDeclaration =
    if (this is KSTypeAlias) findActualType() else this
