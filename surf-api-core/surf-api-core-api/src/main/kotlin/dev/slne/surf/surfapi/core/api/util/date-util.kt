package dev.slne.surf.surfapi.core.api.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
val currentDateTimeFormatted: String
    get() = LocalDateTime.now().format(dateTimeFormatter)