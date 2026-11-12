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
 * Implementation modules are recognised by three name shapes:
 *
 * - `-impl`, which every platform and domain implementation uses
 * - `-internal`, used by `surf-api-internal`, whose contents are a contract between the KSP
 *   processor and the container rather than something a consumer writes against
 * - `-nms-v<digit>`, used by the versioned NMS bridges
 *
 * Everything else counts as an API module. Aggregates are never derived from the name — they are
 * declared by applying the `surf.aggregate` plugin — which matters because `surf-api-minestom-server`
 * is an aggregate whose name would otherwise read as an implementation.
 */
fun surfModuleRole(projectName: String): SurfModuleRole = when {
    projectName.endsWith("-impl") -> SurfModuleRole.IMPL
    projectName.endsWith("-internal") -> SurfModuleRole.IMPL
    NMS_VERSION_MODULE.containsMatchIn(projectName) -> SurfModuleRole.IMPL
    else -> SurfModuleRole.API
}
