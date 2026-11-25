package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.shared.internal.nms.NmsVersion

/**
 * Selects the NMS version module for the running server.
 *
 * The Paper runtime registers the result of [resolve] as a singleton named [BEAN_NAME] in
 * surf-api's root bootstrap context, before the root context scans for version modules.
 * [ConditionalOnMinecraftVersion] reads that bean, so exactly one version module contributes beans.
 */
object NmsVersionResolver {

    /**
     * The name of the bean that holds the selected [NmsVersion].
     */
    const val BEAN_NAME = "surfNmsVersion"

    private val log = logger()

    /**
     * Selects the bindings for a server reporting [minecraftVersion] through [NmsVersion.select].
     *
     * A server without bindings of its own version runs on the newest older ones; a warning names
     * both versions.
     *
     * @param minecraftVersion the version the server reports, such as `26.3.1`
     * @return the selected version
     * @throws IllegalStateException if the server is older than every supported version
     */
    fun resolve(minecraftVersion: String): NmsVersion {
        val selected = checkNotNull(NmsVersion.select(minecraftVersion)) {
            "surf-api has no NMS bindings for Minecraft $minecraftVersion; the oldest supported " +
                "version is ${NmsVersion.NEWEST_FIRST.last().versionPrefix}"
        }

        if (!selected.matches(minecraftVersion)) {
            log.atWarning()
                .log(
                    "No NMS bindings for Minecraft %s, using the newest older ones: %s. A reflected " +
                        "member that changed since then fails where it is used.",
                    minecraftVersion,
                    selected.versionPrefix,
                )
        }

        return selected
    }
}
