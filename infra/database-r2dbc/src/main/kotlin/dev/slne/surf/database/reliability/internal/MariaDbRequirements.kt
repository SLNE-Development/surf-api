package dev.slne.surf.database.reliability.internal

/**
 * Server requirements of the reliability tables on MariaDB: `FOR UPDATE SKIP LOCKED` (MariaDB 10.6) and InnoDB
 * row locks.
 */
internal object MariaDbRequirements {
    private const val MIN_MAJOR = 10
    private const val MIN_MINOR = 6
    private const val REQUIRED_ENGINE = "InnoDB"
    private val VERSION_PREFIX = Regex("""^(\d+)\.(\d+)""")

    /**
     * @param version Result of `SELECT VERSION()`, for example `11.4.2-MariaDB-ubu2404`.
     * @throws IllegalStateException if the server is not MariaDB or older than 10.6
     */
    fun requireSupportedVersion(version: String) {
        check(version.contains("MariaDB", ignoreCase = true)) {
            "surf reliability tables need MariaDB $MIN_MAJOR.$MIN_MINOR or newer, but the server reports '$version'"
        }

        val (major, minor) = checkNotNull(VERSION_PREFIX.find(version)) {
            "Cannot parse MariaDB version '$version'"
        }.destructured

        check(major.toInt() > MIN_MAJOR || major.toInt() == MIN_MAJOR && minor.toInt() >= MIN_MINOR) {
            "surf reliability tables need MariaDB $MIN_MAJOR.$MIN_MINOR or newer for FOR UPDATE SKIP LOCKED, " +
                    "but the server is $version"
        }
    }

    /**
     * @param engines Storage engine per table, `null` for a table that does not exist.
     * @throws IllegalStateException if a table is missing or does not use InnoDB
     */
    fun requireInnoDb(engines: Map<String, String?>) {
        val wrong = engines.filterValues { !it.equals(REQUIRED_ENGINE, ignoreCase = true) }
        check(wrong.isEmpty()) {
            "surf reliability tables must use $REQUIRED_ENGINE, found " +
                    wrong.entries.joinToString { (table, engine) -> "$table: ${engine ?: "missing"}" }
        }
    }
}
