package dev.slne.surf.api.core.messages.pagination

import net.kyori.adventure.text.format.Style

data class PageButton(
    val text: String,
    val enabledStyle: Style,
    val disabledStyle: Style,
)