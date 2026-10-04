package dev.slne.surf.api.minestom.server.visibility

/**
 * Selects player entity IDs using retention-weighted squared distances and hysteresis-based
 * visibility limiting.
 *
 * Candidates are ranked by ascending adjusted distance. Previously visible candidates have
 * their squared distance multiplied by the square of [retentionDistanceFactor]. Equal scores
 * favor previously visible candidates, then candidates with lower entity IDs.
 *
 * When limiting is inactive, all valid candidates are selected. When limiting is active,
 * at most [maxVisible] candidates are selected. Results are sorted by entity ID, not distance.
 *
 * Each selection pass consists of [begin], calls to [offer], and [finish]. Entity IDs must
 * be offered at most once per pass; duplicate offers are not deduplicated.
 *
 * Instances reuse internal buffers and are not thread-safe.
 *
 * @param maxVisible Maximum number of candidates selected while limiting is active.
 * Must be greater than zero.
 * @param activateAt Minimum valid candidate count that activates limiting when it was
 * previously inactive. Must be greater than or equal to [maxVisible].
 * @param deactivateBelow Valid candidate count below which active limiting is disabled.
 * Must be in `1..maxVisible` and strictly less than [activateAt].
 * @param retentionDistanceFactor Distance multiplier for previously visible candidates,
 * applied as its square to squared distances. Must be in `(0.0, 1.0]`. Smaller values
 * increase the retention preference; `1.0` preserves only the equal-score preference.
 * @throws IllegalArgumentException If any constructor argument violates its stated constraints.
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
     * Storage required for either a limited selection or an unlimited selection just below
     * the activation threshold.
     */
    private val capacity = maxOf(maxVisible, activateAt - 1)

    private val ids = IntArray(capacity)
    private val scores = DoubleArray(capacity)
    private val retained = BooleanArray(capacity)
    private val output = IntArray(capacity)

    /**
     * Multiplier applied to squared distances of previously visible candidates.
     */
    private val retentionScoreFactor = retentionDistanceFactor * retentionDistanceFactor

    private var size = 0
    private var count = 0
    private var previouslyVisible = IntArray(0)

    /**
     * Whether visibility limiting is active.
     *
     * Initialized to the previous pass's state by [begin] and updated according to the
     * current pass's valid candidate count by [finish]. Calls to [offer] do not change it.
     */
    var limited: Boolean = false
        private set

    /**
     * Starts a selection pass, discarding candidates and counts from the preceding pass.
     *
     * The previous selection supplies retention preferences only; those entities must
     * still be offered during this pass to be eligible for selection.
     *
     * [previouslyVisible] is stored by reference and is never modified by this selector.
     * It must remain unchanged until [finish] returns and may be returned directly when
     * the selection is unchanged.
     *
     * @param previouslyVisible Previously selected entity IDs, sorted in ascending order.
     * Use an empty array when there is no previous selection.
     * @param wasLimited Whether visibility limiting was active in the preceding pass.
     */
    fun begin(previouslyVisible: IntArray, wasLimited: Boolean) {
        this.previouslyVisible = previouslyVisible

        limited = wasLimited
        size = 0
        count = 0
    }

    /**
     * Considers a candidate for the current selection pass.
     *
     * Non-finite or negative distances are ignored without affecting the selection or
     * the candidate count used for limiting thresholds. Every valid offer contributes
     * to that count, even when the candidate is not retained in the bounded heap.
     *
     * Previously visible candidates receive the configured retention preference.
     * The heap keeps the best-ranked candidates needed to produce the final selection.
     *
     * Call after [begin] and before [finish]. Duplicate entity IDs are not deduplicated.
     *
     * @param entityId Candidate entity ID, which must be offered at most once per pass.
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
     * Completes the current pass, updates [limited], and returns the selected entity IDs.
     *
     * If limiting was active, it remains active when the valid candidate count is at least
     * [deactivateBelow]. Otherwise, it becomes active when that count reaches [activateAt].
     *
     * While limiting is active, the best-ranked candidates are selected up to [maxVisible].
     * Otherwise, all valid candidates are selected, which may exceed [maxVisible].
     *
     * @return Selected entity IDs sorted in ascending order. Returns the array supplied
     * to [begin] when its contents match the selection; otherwise returns a new array.
     * The internal output buffer is never returned directly.
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
     * Replaces the heap root with a candidate and restores worst-first heap ordering.
     *
     * The active heap must be nonempty, and both child subtrees must already satisfy
     * the heap ordering. The heap size is not changed.
     *
     * @param entityId Replacement candidate's entity ID.
     * @param score Replacement candidate's retention-adjusted squared distance.
     * @param keep Whether the replacement candidate was previously visible.
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

    /**
     * Copies a candidate's ID, score, and retention flag between heap slots.
     *
     * @param from Source heap index.
     * @param to Destination heap index.
     */
    private fun copy(from: Int, to: Int) {
        ids[to] = ids[from]
        scores[to] = scores[from]
        retained[to] = retained[from]
    }

    /**
     * Writes a candidate to a heap slot without adjusting heap ordering or size.
     *
     * @param index Destination heap index.
     * @param entityId Candidate's entity ID.
     * @param score Candidate's retention-adjusted squared distance.
     * @param keep Whether the candidate was previously visible.
     */
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
     * Determines whether the first candidate ranks below the second.
     *
     * Higher scores are worse. Equal scores favor previously visible candidates,
     * followed by candidates with lower entity IDs.
     *
     * @param scoreA First candidate's retention-adjusted squared distance.
     * @param retainedA Whether the first candidate was previously visible.
     * @param idA First candidate's entity ID.
     * @param scoreB Second candidate's retention-adjusted squared distance.
     * @param retainedB Whether the second candidate was previously visible.
     * @param idB Second candidate's entity ID.
     * @return `true` if the first candidate is strictly worse; `false` otherwise.
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