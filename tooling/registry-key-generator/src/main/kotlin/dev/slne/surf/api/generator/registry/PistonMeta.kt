package dev.slne.surf.api.generator.registry

import kotlinx.serialization.Serializable

internal const val VERSION_MANIFEST_URL =
    "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

/**
 * The parts of Mojang's version manifest that locate a version's details.
 */
@Serializable
internal data class VersionManifest(val versions: List<Version>) {
    @Serializable
    data class Version(val id: String, val url: String)
}

/**
 * The downloads of a version, keyed by `client`, `server` and the other artifact names.
 */
@Serializable
internal data class VersionDetails(val downloads: Map<String, Download>) {
    @Serializable
    data class Download(val sha1: String, val url: String)
}
