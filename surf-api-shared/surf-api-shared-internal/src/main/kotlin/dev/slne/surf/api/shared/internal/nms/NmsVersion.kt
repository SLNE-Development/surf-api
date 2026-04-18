package dev.slne.surf.api.shared.internal.nms

/**
 * Represents the supported Minecraft NMS versions.
 *
 * @property versionPrefix The version prefix used to match `Bukkit.getMinecraftVersion()`.
 */
enum class NmsVersion(val versionPrefix: String) {
    V1_21_11("1.21.11"),
    V26_1("26.1");
}