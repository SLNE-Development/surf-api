package dev.slne.surf.nats.gradle.weaver

/**
 * Version ordering of the weaver registry.
 *
 * Numeric parts compare first, with missing parts treated as 0. At equal numeric parts a release sorts above
 * any qualified version, and qualifiers compare segment by segment (numerically when both segments are numeric).
 */
internal object WeaverVersions {

    private const val SNAPSHOT_SUFFIX = "-SNAPSHOT"
    private val NUMERIC = Regex("""^\d+$""")

    val comparator: Comparator<String> = Comparator(::compare)

    fun compare(a: String, b: String): Int {
        val (numbersA, qualifierA) = split(a)
        val (numbersB, qualifierB) = split(b)

        for (i in 0 until maxOf(numbersA.size, numbersB.size)) {
            val result = numbersA.getOrElse(i) { 0L }.compareTo(numbersB.getOrElse(i) { 0L })
            if (result != 0) return result
        }

        return when {
            qualifierA == null && qualifierB == null -> 0
            qualifierA == null -> 1
            qualifierB == null -> -1
            else -> compareQualifiers(qualifierA, qualifierB)
        }
    }

    /**
     * Returns the highest release in [versions] strictly below [projectVersion] without its `-SNAPSHOT` suffix,
     * or `null` when there is none.
     */
    fun previousRelease(versions: Collection<String>, projectVersion: String): String? {
        val current = projectVersion.removeSuffix(SNAPSHOT_SUFFIX)
        return versions
            .filter { !it.endsWith(SNAPSHOT_SUFFIX) && compare(it, current) < 0 }
            .maxWithOrNull(comparator)
    }

    private fun split(version: String): Pair<List<Long>, String?> {
        val dash = version.indexOf('-')
        val core = if (dash == -1) version else version.substring(0, dash)
        val qualifier = if (dash == -1) null else version.substring(dash + 1)
        return core.split('.').map { it.toLongOrNull() ?: 0L } to qualifier
    }

    private fun compareQualifiers(a: String, b: String): Int {
        val segmentsA = a.split('.')
        val segmentsB = b.split('.')

        for (i in 0 until minOf(segmentsA.size, segmentsB.size)) {
            val x = segmentsA[i]
            val y = segmentsB[i]
            val result = if (NUMERIC.matches(x) && NUMERIC.matches(y)) {
                x.toBigInteger().compareTo(y.toBigInteger())
            } else {
                x.compareTo(y)
            }
            if (result != 0) return result
        }

        return segmentsA.size.compareTo(segmentsB.size)
    }
}
