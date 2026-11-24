package dev.slne.surf.microservice.gradle.mainclass

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File

private const val MAIN_NAME = "main"
private const val MAIN_DESCRIPTOR = "([Ljava/lang/String;)V"
private const val MAIN_ACCESS = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC
private const val SYNTHETIC_CLASS = Opcodes.ACC_SYNTHETIC
private const val MODULE_INFO = "module-info"
private const val MULTI_RELEASE_PREFIX = "META-INF/"

/**
 * Finds the classes with a `public static void main(String[])` among the class files of
 * [classDirectories], as binary names sorted alphabetically.
 *
 * Reads only the class headers and method declarations, so no class is loaded. A Kotlin top-level
 * `fun main(args: Array<String>)` compiles to such a method in the file class (`MainKt`). Synthetic
 * methods count: the `main()` without parameters of Kotlin gets a synthetic bridge with the array
 * parameter, which is what the JVM runs.
 *
 * Synthetic classes are skipped. Under `@JvmMultifileClass`, a `main` appears in the public facade
 * and again in the synthetic part class (`Main__AppKt`); only the facade is an entry point.
 */
internal fun findMainClasses(classDirectories: Iterable<File>): List<String> {
    return classDirectories
        .filter { it.isDirectory }
        .flatMap { directory ->
            directory.walkTopDown().filter { it.isClassFile(directory) }.mapNotNull(
                ::mainClassOf,
            )
        }
        .distinct()
        .sorted()
}

private fun File.isClassFile(root: File): Boolean {
    if (!isFile || extension != "class" || nameWithoutExtension == MODULE_INFO) return false

    return !relativeTo(root).invariantSeparatorsPath.startsWith(MULTI_RELEASE_PREFIX)
}

private fun mainClassOf(classFile: File): String? {
    val visitor = MainMethodVisitor()
    classFile.inputStream().use { ClassReader(it) }
        .accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

    return visitor.className.takeIf { visitor.hasMain && !visitor.isSynthetic }
}

private class MainMethodVisitor : ClassVisitor(Opcodes.ASM9) {
    var className: String = ""
        private set
    var hasMain = false
        private set
    var isSynthetic = false
        private set

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?,
    ) {
        className = name.replace('/', '.')
        isSynthetic = access and SYNTHETIC_CLASS != 0
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor? {
        if (name == MAIN_NAME &&
            descriptor == MAIN_DESCRIPTOR &&
            access and MAIN_ACCESS == MAIN_ACCESS
        ) {
            hasMain = true
        }

        return null
    }
}
