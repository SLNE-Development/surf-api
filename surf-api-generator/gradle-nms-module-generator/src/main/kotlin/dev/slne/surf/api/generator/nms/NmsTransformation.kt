package dev.slne.surf.api.generator.nms

/**
 * A single source-code transformation applied during NMS module generation.
 *
 * Transformations run **after** the automatic version-id and class-prefix
 * replacements and are applied in declaration order.
 */
sealed class NmsTransformation {

    /**
     * Renames a fully-qualified class in every generated file.
     *
     * Handles both `import` statements and in-code simple-name references
     * (via `\b` word-boundary matching when the simple names differ).
     *
     * @property oldFqn Fully-qualified name of the class in the reference module.
     * @property newFqn Fully-qualified name of the replacement class.
     */
    data class RenameClass(
        val oldFqn: String,
        val newFqn: String,
    ) : NmsTransformation() {
        val oldSimpleName: String get() = oldFqn.substringAfterLast('.')
        val newSimpleName: String get() = newFqn.substringAfterLast('.')
    }

    /**
     * Removes an `import` line from every generated file.
     *
     * @property fqn Fully-qualified name to remove (without the `import` keyword).
     */
    data class RemoveImport(val fqn: String) : NmsTransformation()

    /**
     * Literal string replacement applied to every generated file.
     *
     * @property old Text to search for.
     * @property new Replacement text.
     */
    data class ReplaceCode(val old: String, val new: String) : NmsTransformation()

    /**
     * Regex-based replacement applied to every generated file.
     *
     * @property pattern Compiled regex pattern.
     * @property replacement Replacement string (supports group references like `$1`).
     */
    data class ReplacePattern(val pattern: Regex, val replacement: String) : NmsTransformation()

    /**
     * Custom transformation function targeting files whose relative path
     * matches or ends with [filePattern].
     *
     * @property filePattern Relative path suffix to match (e.g. `"bridges/SomeFile.kt"`).
     * @property transformer Receives the current file content and returns the transformed content.
     */
    data class TransformFile(
        val filePattern: String,
        val transformer: (String) -> String,
    ) : NmsTransformation()

    /**
     * Excludes matching files from generation entirely.
     *
     * @property filePattern Relative path suffix to match.
     */
    data class ExcludeFile(val filePattern: String) : NmsTransformation()
}

