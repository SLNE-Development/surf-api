package dev.slne.surf.api.shared.internal.nms

/**
 * The Minecraft versions surf-api ships NMS bindings for.
 *
 * @property versionPrefix the version this entry binds against, as Minecraft writes it
 */
enum class NmsVersion(val versionPrefix: String) {
    V1_21_11("1.21.11"),
    V26_1("26.1"),
    V26_2("26.2"),
    V26_3("26.3");

    /**
     * [versionPrefix] as numbers, so versions compare as versions.
     *
     * The whole point of keeping this is that a dotted version is not a string: `"26.10"` is above
     * `"26.3"` and sorts below it, and `"26.10"` *starts with* `"26.1"`. Both of those were being
     * relied on, and both are wrong — see [matches] and [NEWEST_FIRST].
     */
    val components: List<Int> = versionPrefix.split('.').map(String::toInt)

    /**
     * Whether a server reporting [minecraftVersion] should use this entry's bindings.
     *
     * True when this entry's components are a *component-wise* prefix of the server's. That is the
     * fix for the trap in the obvious implementation: `"26.10".startsWith("26.1")` is true, so a
     * 26.10 server silently selected the 26.1 bindings and then failed at whichever reflected
     * member happened to have moved — nowhere near the cause. Comparing component by component,
     * `26.10` is not `26.1` and never matches it.
     *
     * A server version with fewer components than this entry never matches: `26` is not `26.3`.
     */
    fun matches(minecraftVersion: String): Boolean {
        val reported = runCatching { minecraftVersion.split('.').map(String::toInt) }.getOrNull()
            ?: return false

        if (reported.size < components.size) return false

        return components.indices.all { reported[it] == components[it] }
    }

    companion object {

        /**
         * The entries from newest to oldest, by number rather than by text.
         *
         * `maxByOrNull { it.versionPrefix }` reads like it finds the newest and does not: with
         * `26.10` present it answers `26.3`, because `'3' > '1'`. A fallback that picks the wrong
         * entry is worse than no fallback, since it starts the server on bindings for another
         * version.
         */
        val NEWEST_FIRST: List<NmsVersion> =
            entries.sortedWith(compareByDescending(COMPONENT_ORDER) { it.components })

        /** The newest version surf-api has bindings for. */
        val NEWEST: NmsVersion get() = NEWEST_FIRST.first()

        /**
         * The entry a server reporting [minecraftVersion] should use, or `null` for no match.
         *
         * Newest first, so a server version that could match two entries — which only happens if
         * one is a component-wise prefix of the other — gets the more specific one.
         */
        fun matching(minecraftVersion: String): NmsVersion? =
            NEWEST_FIRST.firstOrNull { it.matches(minecraftVersion) }
    }
}

/**
 * Orders version components as numbers, shorter first when one is a prefix of the other.
 *
 * Declared beside the enum rather than inside it because a companion's initialiser cannot read it
 * while the companion is still being built, and `internal` rather than private so a test can feed
 * it versions the enum does not list. That matters: with the four entries that exist today a plain
 * text sort produces the same order, so nothing asserted about this enum can tell the two apart.
 * The case that does is `26.10` against `26.3`.
 */
internal val COMPONENT_ORDER: Comparator<List<Int>> = Comparator { left, right ->
    val shared = minOf(left.size, right.size)

    for (index in 0 until shared) {
        val difference = left[index].compareTo(right[index])
        if (difference != 0) return@Comparator difference
    }

    left.size.compareTo(right.size)
}
