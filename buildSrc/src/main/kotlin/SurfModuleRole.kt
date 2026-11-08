/**
 * The role a subproject plays in the module layout.
 *
 * @see surfModuleRole
 */
enum class SurfModuleRole {
    /** Declares contracts that consumers compile against. */
    API,

    /** Provides implementations; wired in at runtime and shaded into the final jar. */
    IMPL,

    /** Holds no sources and only groups other modules for consumers. */
    AGGREGATE,
}

private val NMS_VERSION_MODULE = Regex("-nms-v[0-9]")

/**
 * Classifies a subproject by its name.
 *
 * Implementation modules are recognised by four name shapes, because the layout currently uses
 * more than one spelling for the same role:
 *
 * - `-impl`, the target spelling
 * - `-server`, used by the platform implementations and the Minestom feature modules
 * - `-internal`, used by `surf-api-shared-internal`
 * - `-nms-v<digit>`, used by the versioned NMS bridges
 *
 * Everything else counts as an API module. Aggregates are never derived from the name; they are
 * declared by applying the `surf.aggregate` plugin.
 */
fun surfModuleRole(projectName: String): SurfModuleRole = when {
    projectName.endsWith("-impl") -> SurfModuleRole.IMPL
    projectName.contains("-server") -> SurfModuleRole.IMPL
    projectName.endsWith("-internal") -> SurfModuleRole.IMPL
    NMS_VERSION_MODULE.containsMatchIn(projectName) -> SurfModuleRole.IMPL
    else -> SurfModuleRole.API
}
