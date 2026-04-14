package dev.slne.surf.api.gen.nms

/**
 * Configuration for a target NMS version.
 *
 * @property versionId      Short identifier used in package/class names, e.g. "v1_21_11"
 * @property classPrefix    Prefix for generated class names, e.g. "V1_21_11"
 * @property sourceModulePath  Relative path from repo root to the target module's source root
 * @property importReplacements  Map of import replacements (old → new) applied to every file
 * @property codeReplacements    Map of code-level string replacements applied after import replacement
 * @property fileTransformers    Per-file transformers keyed by relative source path (from the reference module)
 */
data class NmsVersionConfig(
    val versionId: String,
    val classPrefix: String,
    val sourceModulePath: String,
    val importReplacements: Map<String, String> = emptyMap(),
    val codeReplacements: Map<String, String> = emptyMap(),
    val fileTransformers: Map<String, (String) -> String> = emptyMap(),
)
