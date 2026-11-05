package dev.slne.surf.microservice.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import dev.slne.surf.microservice.processor.declaration.jvmBinaryName

/**
 * Classes of one kind found across all processing rounds.
 *
 * Stores names and source files only, never the declarations, because KSP invalidates symbols after their round.
 */
internal class DiscoveredClasses {
    private val classes = HashMap<String, DiscoveredClass>()

    val size: Int
        get() = classes.size

    /**
     * Binary names in lexicographic order.
     */
    val binaryNames: List<String>
        get() = classes.keys.sorted()

    /**
     * Qualified source names in lexicographic order, for messages.
     */
    val qualifiedNames: List<String>
        get() = classes.values.map { it.qualifiedName }.sorted()

    fun add(declaration: KSClassDeclaration) {
        val binaryName = declaration.jvmBinaryName()
        classes[binaryName] = DiscoveredClass(
            qualifiedName = declaration.qualifiedName?.asString() ?: binaryName,
            sourceFile = declaration.containingFile,
        )
    }

    /**
     * Aggregating dependencies on the source files of all discovered classes.
     *
     * An aggregating output is regenerated whenever any input changes, which covers classes that are added later.
     */
    fun dependencies(): Dependencies =
        Dependencies(aggregating = true, *classes.values.mapNotNull { it.sourceFile }.distinct().toTypedArray())

    private class DiscoveredClass(val qualifiedName: String, val sourceFile: KSFile?)
}
