package dev.slne.surf.microservice.processor.declaration

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isInternal
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin

/**
 * Returns why the runtime cannot instantiate this class reflectively, or an empty list if it can.
 *
 * The runtime uses the `INSTANCE` field of an `object` and the public no-argument constructor of a class. Both must be
 * reachable from another package, so the class and every enclosing class must be public or `internal`, which is public
 * in bytecode.
 */
internal fun KSClassDeclaration.instantiationProblems(): List<String> = buildList {
    if (!enclosingClassesAndSelf().all { it.isPublic() || it.isInternal() }) {
        add("it and every class enclosing it must be public or internal")
    }
    when {
        isCompanionObject -> add("a companion object has no INSTANCE field; use a top-level or nested object")
        isInner() -> add("an inner class needs an instance of its outer class; make it a nested (static) class")
        classKind == ClassKind.CLASS && !hasPublicNoArgumentConstructor() ->
            add("it needs a public constructor without parameters")
    }
}

/**
 * KSP marks Kotlin inner classes with [Modifier.INNER], but Java inner classes only by the absence of
 * [Modifier.JAVA_STATIC]; members of Java interfaces carry [Modifier.JAVA_STATIC] implicitly.
 */
private fun KSClassDeclaration.isInner(): Boolean =
    Modifier.INNER in modifiers ||
        (origin == Origin.JAVA && parentDeclaration is KSClassDeclaration && Modifier.JAVA_STATIC !in modifiers)

private fun KSClassDeclaration.hasPublicNoArgumentConstructor(): Boolean =
    getConstructors().any { it.isPublic() && it.isCallableWithoutArguments(primaryConstructor) }

/**
 * Kotlin generates a JVM no-argument constructor only for a primary constructor whose parameters all have defaults.
 */
private fun KSFunctionDeclaration.isCallableWithoutArguments(primaryConstructor: KSFunctionDeclaration?): Boolean =
    parameters.isEmpty() || (this == primaryConstructor && parameters.all { it.hasDefault })
