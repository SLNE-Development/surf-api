package dev.slne.surf.api.messages.pagination

import net.kyori.adventure.text.format.Style

data class PageButton(
    val text: String,
    val enabledStyle: Style,
    val disabledStyle: Style,
)