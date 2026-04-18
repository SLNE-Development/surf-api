package dev.slne.surf.api.generator.nms

/**
 * DSL scope for declaring [NmsTransformation] rules.
 *
 * Available inside the `transformations { … }` block of the
 * [NmsGeneratorExtension]. Each method adds a transformation that will
 * be applied in declaration order during generation.
 */
@NmsGeneratorDsl
class TransformationScope internal constructor() {

    private val _transformations = mutableListOf<NmsTransformation>()

    /** Snapshot of all registered transformations. */
    internal val transformations: List<NmsTransformation> get() = _transformations.toList()

    /**
     * Renames a fully-qualified class in all generated files.
     *
     * Automatically rewrites the `import` line **and** every in-code
     * reference (via word-boundary matching when the simple names differ).
     */
    fun renameClass(oldFqn: String, newFqn: String) {
        _transformations += NmsTransformation.RenameClass(oldFqn, newFqn)
    }

    /** Removes the `import` line for [fqn] from all generated files. */
    fun removeImport(fqn: String) {
        _transformations += NmsTransformation.RemoveImport(fqn)
    }

    /** Replaces every occurrence of [old] with [new] in all generated files. */
    fun replaceCode(old: String, new: String) {
        _transformations += NmsTransformation.ReplaceCode(old, new)
    }

    /** Replaces matches of the given regex [pattern] with [replacement]. */
    fun replacePattern(pattern: String, replacement: String) {
        _transformations += NmsTransformation.ReplacePattern(Regex(pattern), replacement)
    }

    /**
     * Applies [transformer] to every file whose relative path ends with
     * [filePattern] (uses `/` as separator).
     */
    fun transformFile(filePattern: String, transformer: (String) -> String) {
        _transformations += NmsTransformation.TransformFile(filePattern, transformer)
    }

    /** Excludes files whose relative path ends with [filePattern] from generation. */
    fun excludeFile(filePattern: String) {
        _transformations += NmsTransformation.ExcludeFile(filePattern)
    }
}

