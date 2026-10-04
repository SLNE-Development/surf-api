package dev.slne.surf.api.minestom.server.plugins.impl

/**
 * Orders plugins so that every plugin comes after the plugins it depends on.
 */
internal object PluginLoadOrder {

    /**
     * The result of ordering: the plugins that can be loaded, in order, and why the others cannot.
     */
    data class Result(
        val ordered: List<PluginCandidate>,
        val rejected: Map<String, String>,
    )

    fun order(candidates: List<PluginCandidate>): Result {
        val rejected = linkedMapOf<String, String>()
        val byId = linkedMapOf<String, PluginCandidate>()

        for (candidate in candidates) {
            val id = candidate.meta.id
            val existing = byId[id]
            if (existing != null) {
                rejected["$id (${candidate.source})"] = "the id is already taken by ${existing.source}"
                continue
            }
            byId[id] = candidate
        }

        // Drop plugins whose required dependencies are missing, until nothing changes
        var changed = true
        while (changed) {
            changed = false
            for ((id, candidate) in byId.entries.toList()) {
                val missing = candidate.meta.dependencies
                    .filter { dependency -> !dependency.optional && dependency.id !in byId }
                    .map { it.id }

                if (missing.isNotEmpty()) {
                    byId.remove(id)
                    rejected[id] = "it depends on ${missing.joinToString()}, which " +
                            (if (missing.size == 1) "is" else "are") + " not available"
                    changed = true
                }
            }
        }

        val ordered = mutableListOf<PluginCandidate>()
        val state = mutableMapOf<String, VisitState>()

        fun visit(candidate: PluginCandidate, path: List<String>): Boolean {
            val id = candidate.meta.id
            when (state[id]) {
                VisitState.DONE -> return true
                VisitState.FAILED -> return false
                VisitState.VISITING -> {
                    val cycle = path.dropWhile { it != id } + id
                    cycle.forEach { member ->
                        state[member] = VisitState.FAILED
                        rejected[member] = "of the dependency cycle ${cycle.joinToString(" -> ")}"
                    }
                    return false
                }

                null -> Unit
            }

            state[id] = VisitState.VISITING
            for (dependency in candidate.meta.dependencies) {
                val target = byId[dependency.id] ?: continue
                if (!visit(target, path + id)) {
                    if (state[id] != VisitState.FAILED) {
                        state[id] = VisitState.FAILED
                        rejected.putIfAbsent(id, "its dependency ${dependency.id} cannot be loaded")
                    }
                    return false
                }
            }

            if (state[id] == VisitState.FAILED) return false

            state[id] = VisitState.DONE
            ordered += candidate
            return true
        }

        byId.values.forEach { candidate -> visit(candidate, emptyList()) }

        return Result(ordered, rejected)
    }

    private enum class VisitState { VISITING, DONE, FAILED }
}
