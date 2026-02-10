package dev.slne.surf.surfapi.core.api.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A date-time formatter that formats dates and times in the pattern "dd.MM.yyyy HH:mm".
 *
 * For example: "10.02.2026 19:30"
 */
val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

/**
 * Returns the current date and time formatted as "dd.MM.yyyy HH:mm".
 *
 * This property retrieves the current system time each time it is accessed.
 */
val currentDateTimeFormatted: String
    get() = LocalDateTime.now().format(dateTimeFormatter)