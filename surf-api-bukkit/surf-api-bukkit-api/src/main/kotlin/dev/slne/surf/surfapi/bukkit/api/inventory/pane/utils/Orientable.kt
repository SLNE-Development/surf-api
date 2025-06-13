package dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils

interface Orientable {

    var orientation: Orientation

    enum class Orientation {
        HORIZONTAL,
        VERTICAL
    }
}