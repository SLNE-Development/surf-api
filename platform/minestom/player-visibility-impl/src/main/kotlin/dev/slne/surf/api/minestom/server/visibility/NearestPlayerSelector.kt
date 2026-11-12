package dev.slne.surf.api.minestom.server.visibility

/**
 * Selects the nearest player entity IDs for one viewer, with hysteresis on when the limit applies.
 *
 * Candidates rank by ascending score. The score is the squared distance, multiplied by the square
 * of [retentionDistanceFactor] for previously visible candidates. Equal scores favor previously
 * visible candidates, then lower entity IDs.
 *
 * While limiting is inactive, all valid candidates are selected. While it is active, at most
 * [maxVisible] are. Results are sorted by entity ID, not by distance.
 *
 * A pass is [begin], any number of [offer] calls, then [finish]. Each entity ID must be offered at
 * most once per pass; duplicates are not detected. Instances reuse internal buffers and are not
 * thread-safe.
 *
 * @param maxVisible Maximum selection size while limiting is active. Must be greater than zero.
 * @param activateAt Valid candidate count that activates limiting. Must be at least [maxVisible].
 * @param deactivateBelow Valid candidate count below which active limiting ends. Must be in
 * `1..maxVisible` and less than [activateAt].
 * @param retentionDistanceFactor Distance multiplier for previously visible candidates, applied
 * squared to squared distances. Must be in `(0.0, 1.0]`; `1.0` leaves only the equal-score
 * preference.
 * @throws IllegalArgumentException If a constructor argument violates its constraint.
 */
internal class NearestPlayerSelector(
    private val maxVisible: Int,
    private val activateAt: Int,
    private val deactivateBelow: Int,
    retentionDistanceFactor: Double,
) {
    init {
        require(maxVisible > 0) { "maxVisible must be greater than 0" }
        require(activateAt >= maxVisible) { "activateAt must be greater than or equal to maxVisible" }
        require(deactivateBelow in 1..maxVisible && deactivateBelow < activateAt) { "deactivateBelow must be between 1 and maxVisible, and less than activateAt" }
        require(retentionDistanceFactor > 0.0 && retentionDistanceFactor <= 1.0) { "retentionDistanceFactor must be between 0.0 and 1.0" }
    }

    /**
     * Largest result: a limited selection, or an unlimited one just below [activateAt].
     */
    private val capacity = maxOf(maxVisible, activateAt - 1)

    private val ids = IntArray(capacity)
    private val scores = DoubleArray(capacity)
    private val retained = BooleanArray(capacity)
    private val output = IntArray(capacity)

    private val retentionScoreFactor = retentionDistanceFactor * retentionDistanceFactor

    private var size = 0
    private var count = 0
    private var previouslyVisible = IntArray(0)

    /**
     * Whether limiting is active.
     *
     * [begin] sets it to the previous pass's state and [finish] updates it from the valid candidate
     * count. [offer] does not change it.
     */
    var limited: Boolean = false
        private set

    /**
     * Starts a pass and discards the candidates of the previous one.
     *
     * [previouslyVisible] only sets retention preference; those entities must be offered again to
     * be selected. The selector keeps the array by reference, never modifies it, and may return it
     * from [finish]. The caller must not modify it until [finish] returns.
     *
     * @param previouslyVisible Previously selected entity IDs in ascending order, or an empty array.
     * @param wasLimited Whether limiting was active in the previous pass.
     */
    fun begin(previouslyVisible: IntArray, wasLimited: Boolean) {
        this.previouslyVisible = previouslyVisible

        limited = wasLimited
        size = 0
        count = 0
    }

    /**
     * Offers a candidate to the current pass.
     *
     * Non-finite and negative distances are ignored and do not count toward the activation
     * thresholds. Every valid offer counts, including candidates the bounded heap drops.
     *
     * @param entityId Candidate entity ID; offer each ID at most once per pass.
     * @param distanceSquared Squared distance from the viewer to the candidate.
     */
    fun offer(entityId: Int, distanceSquared: Double) {
        if (!distanceSquared.isFinite() || distanceSquared < 0.0) return

        count++

        val keep = previouslyVisible.binarySearch(entityId) >= 0
        val score = distanceSquared * if (keep) retentionScoreFactor else 1.0

        if (size < capacity) {
            var index = size++

            while (index > 0) {
                val parent = (index - 1) ushr 1

                if (!worse(
                        score,
                        keep,
                        entityId,
                        scores[parent],
                        retained[parent],
                        ids[parent],
                    )
                ) {
                    break
                }

                copy(parent, index)
                index = parent
            }

            put(index, entityId, score, keep)
        } else if (
            worse(
                scores[0],
                retained[0],
                ids[0],
                score,
                keep,
                entityId,
            )
        ) {
            siftDown(entityId, score, keep)
        }
    }

    /**
     * Ends the pass, updates [limited] and returns the selected entity IDs.
     *
     * Active limiting stays active while the valid candidate count is at least [deactivateBelow].
     * Inactive limiting becomes active once the count reaches [activateAt]. An unlimited selection
     * may exceed [maxVisible].
     *
     * @return Selected entity IDs in ascending order. Returns the array passed to [begin] when its
     * contents equal the selection, otherwise a new array. Never exposes the internal buffer.
     */
    fun finish(): IntArray {
        limited = if (limited) {
            count >= deactivateBelow
        } else {
            count >= activateAt
        }

        val resultSize = if (limited) {
            minOf(count, maxVisible)
        } else {
            count
        }

        while (size > resultSize) {
            val last = --size

            if (size > 0) {
                siftDown(ids[last], scores[last], retained[last])
            }
        }

        for (index in 0 until size) {
            output[index] = ids[index]
        }

        output.sort(0, size)

        if (size == previouslyVisible.size) {
            var same = true

            for (index in 0 until size) {
                if (output[index] != previouslyVisible[index]) {
                    same = false
                    break
                }
            }

            if (same) return previouslyVisible
        }

        return output.copyOf(size)
    }

    /**
     * Replaces the heap root with the given candidate and restores the worst-first heap order.
     *
     * Requires a nonempty heap whose child subtrees of the root already satisfy the order.
     */
    private fun siftDown(entityId: Int, score: Double, keep: Boolean) {
        var index = 0

        while (true) {
            val left = index * 2 + 1
            if (left >= size) break

            var child = left
            val right = left + 1

            if (
                right < size &&
                worse(
                    scores[right],
                    retained[right],
                    ids[right],
                    scores[left],
                    retained[left],
                    ids[left],
                )
            ) {
                child = right
            }

            if (!worse(
                    scores[child],
                    retained[child],
                    ids[child],
                    score,
                    keep,
                    entityId,
                )
            ) {
                break
            }

            copy(child, index)
            index = child
        }

        put(index, entityId, score, keep)
    }

    private fun copy(from: Int, to: Int) {
        ids[to] = ids[from]
        scores[to] = scores[from]
        retained[to] = retained[from]
    }

    private fun put(
        index: Int,
        entityId: Int,
        score: Double,
        keep: Boolean,
    ) {
        ids[index] = entityId
        scores[index] = score
        retained[index] = keep
    }

    /**
     * Returns whether candidate A ranks strictly below candidate B.
     *
     * Higher scores are worse. Equal scores favor previously visible candidates, then lower IDs.
     */
    private fun worse(
        scoreA: Double, retainedA: Boolean, idA: Int,
        scoreB: Double, retainedB: Boolean, idB: Int,
    ): Boolean = when {
        scoreA != scoreB -> scoreA > scoreB
        retainedA != retainedB -> !retainedA
        else -> idA > idB
    }
}