package dev.slne.surf.api.processor.util

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName

object AnnotationUtils {

    /**
     * Returns the top-level classes of the module's source files that carry [annotationFqName] directly.
     *
     * Meta-annotations are not followed, and annotation classes are included.
     */
    fun findAnnotatedClasses(
        resolver: Resolver,
        annotationFqName: String,
    ): Sequence<KSClassDeclaration> = resolver.getAllFiles()
        .flatMap { it.declarations }
        .filterIsInstance<KSClassDeclaration>()
        .filter { decl ->
            decl.annotations.any { ann ->
                (ann.annotationType.resolve().declaration as? KSClassDeclaration)
                    ?.qualifiedName?.asString() == annotationFqName
            }
        }
}

fun KSAnnotation.getArgumentValue(name: String): Any? {
    return arguments.find { it.name?.asString() == name }?.value
}

fun KSAnnotation.stringArg(name: String, default: String = ""): String {
    return getArgumentValue(name) as? String ?: default
}

fun KSAnnotation.booleanArg(name: String, default: Boolean): Boolean {
    return getArgumentValue(name) as? Boolean ?: default
}

fun KSAnnotation.intArg(name: String, default: Int): Int {
    return (getArgumentValue(name) as? Number)?.toInt() ?: default
}

fun KSAnnotation.longArg(name: String, default: Long): Long {
    return (getArgumentValue(name) as? Number)?.toLong() ?: default
}

fun KSAnnotation.enumArg(name: String, default: String): String {
    val value = getArgumentValue(name) ?: return default

    return when (value) {
        is KSName -> value.getShortName()
        else -> value.toString().substringAfterLast('.')
    }
}

inline fun <reified T> KSAnnotation.getArgumentValueAs(name: String): T? {
    return getArgumentValue(name) as? T
}
