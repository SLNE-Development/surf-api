package dev.slne.surf.api.shared.internal.nms

/**
 * The Minecraft versions surf-api ships NMS bindings for.
 *
 * This enum is the single list of supported versions: the Paper runtime selects a version module
 * from it, and the NMS module generator in `tooling/nms-module-generator` compiles this
 * file into its own build to name and scaffold the modules. Every entry has a version module under
 * `platform/paper/nms/` whose package segment is the lowercased entry name, such as `v26_3`.
 *
 * @property versionPrefix the version this entry binds against, as Minecraft writes it
 * @property paperDevBundle the `paperweight.paperDevBundle(...)` notation the generator writes
 * into a new module's build file
 */
enum class NmsVersion(val versionPrefix: String, val paperDevBundle: String) {
    V1_21_11("1.21.11", "1.21.11-R0.1-SNAPSHOT"),
    V26_1("26.1", "26.1+"),
    V26_2("26.2", "26.2.build.+"),
    V26_3("26.3", "26.3.build.+");

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
        val reported = parseComponents(minecraftVersion) ?: return false

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
        val NEWEST_FIRST: List<NmsVersion> = entries.sortedWith(
            compareByDescending(COMPONENT_ORDER) { it.components },
        )

        /**
         * The entry a server reporting [minecraftVersion] should use, or `null` for no match.
         *
         * Newest first, so a server version that could match two entries — which only happens if
         * one is a component-wise prefix of the other — gets the more specific one.
         */
        fun matching(minecraftVersion: String): NmsVersion? {
            return NEWEST_FIRST.firstOrNull { it.matches(minecraftVersion) }
        }

        /**
         * Selects the bindings for a server reporting [minecraftVersion].
         *
         * The entry [matching] the server version wins. Otherwise the newest entry older than the
         * server is selected, so a server newer than every entry runs on the newest bindings. A
         * version that does not start with a number, which only a fork reports, gets the newest
         * entry.
         *
         * @param minecraftVersion the version the server reports, such as `26.3.1`
         * @return the selected entry, or `null` if the server is older than every entry
         */
        fun select(minecraftVersion: String): NmsVersion? {
            matching(minecraftVersion)?.let { return it }

            val reported = parseComponents(minecraftVersion) ?: return NEWEST_FIRST.first()

            return NEWEST_FIRST.firstOrNull { COMPONENT_ORDER.compare(it.components, reported) < 0 }
        }
    }
}

/**
 * Splits the leading dotted number of [version] into its components, so `26.4-rc-1` reads as
 * `26.4`, or answers `null` if [version] does not start with a number.
 */
private fun parseComponents(version: String): List<Int>? {
    return LEADING_VERSION.find(version)?.value?.split('.')?.map(String::toInt)
}

private val LEADING_VERSION = Regex("""^\d+(?:\.\d+)*""")

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
