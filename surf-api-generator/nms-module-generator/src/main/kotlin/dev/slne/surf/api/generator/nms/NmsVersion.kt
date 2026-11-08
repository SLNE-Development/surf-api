package dev.slne.surf.api.generator.nms

/**
 * Supported NMS versions for module generation.
 *
 * Each entry encapsulates all version-specific metadata required by the
 * generator, so consumers only need to reference an enum constant instead
 * of specifying identifiers, prefixes, and bundle versions manually.
 *
 * @property versionId Identifier used in package names and directory paths (e.g. `"v26_1"`).
 * @property classPrefix Prefix applied to generated class names (e.g. `"V26_1"`).
 * @property paperDevBundle Version string passed to `paperweight.paperDevBundle(…)`.
 */
enum class NmsVersion(
    val versionId: String,
    val classPrefix: String,
    val paperDevBundle: String,
) {
    V1_21_11(
        versionId = "v1_21_11",
        classPrefix = "V1_21_11",
        paperDevBundle = "1.21.11-R0.1-SNAPSHOT",
    ),
    V26_1(
        versionId = "v26_1",
        classPrefix = "V26_1",
        paperDevBundle = "26.1+",
    ),
    V26_2(
        versionId = "v26_2",
        classPrefix = "V26_2",
        paperDevBundle = "26.2+",
    ),
    V26_3(
        versionId = "v26_3",
        classPrefix = "V26_3",
        paperDevBundle = "26.3.build.+",
    );

    /** Directory name of this version's module, as `settings.gradle.kts` lists it (e.g. `"v26-1"`). */
    val slug: String
        get() = versionId.replace('_', '-')

    /** Relative path from the repository root to this version's Gradle module. */
    val modulePath: String
        get() = "$MODULE_BASE_PATH$slug"

    /**
     * Flat Gradle project path of this version's module (e.g. `":surf-api-paper-nms-v26-1"`).
     *
     * `settings.gradle.kts` attaches each module directory to a single-segment project named after
     * the published artifact, so the project path does not mirror [modulePath].
     */
    val gradlePath: String
        get() = ":$MODULE_NAME_PREFIX$slug"

    private companion object {
        const val MODULE_BASE_PATH = "platform/paper/nms/"
        const val MODULE_NAME_PREFIX = "surf-api-paper-nms-"
    }
}
