package dev.slne.surf.microservice.processor.declaration

/**
 * Returns the JVM binary name of a class, as accepted by `Class.forName`.
 *
 * @param packageName the package, empty for the default package
 * @param classNames the simple names from the outermost class to the class itself
 */
internal fun jvmBinaryName(packageName: String, classNames: List<String>): String {
    require(classNames.isNotEmpty()) { "classNames must contain at least the class itself" }

    val nestedName = classNames.joinToString("$")
    return if (packageName.isEmpty()) nestedName else "$packageName.$nestedName"
}
