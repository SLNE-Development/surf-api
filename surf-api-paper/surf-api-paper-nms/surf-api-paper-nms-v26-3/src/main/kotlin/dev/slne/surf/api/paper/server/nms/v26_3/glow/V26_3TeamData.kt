package dev.slne.surf.api.paper.server.nms.v26_3.glow

import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import net.minecraft.world.scores.TeamColor
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Suppress("ClassName")
class V26_3TeamData(color: TeamColor) {
    private val scoreboard = Scoreboard()
    val teamId = "glow-${uid()}${color.ordinal}"
    val team = PlayerTeam(scoreboard, teamId).apply {
        collisionRule = Team.CollisionRule.NEVER
        this.color = Optional.of(color)
    }

    private val seenBy = ConcurrentHashMap.newKeySet<UUID>()

    fun markSeen(uniqueId: UUID) = seenBy.add(uniqueId)
    fun isSeen(uniqueId: UUID): Boolean = seenBy.contains(uniqueId)
    fun removeSeen(uniqueId: UUID) = seenBy.remove(uniqueId)

    companion object {
        private val lastUid = AtomicInteger()
        fun uid(): Int = lastUid.getAndIncrement()

        private val teams = EnumMap<TeamColor, V26_3TeamData>(TeamColor::class.java)

        fun getByColor(color: TeamColor): V26_3TeamData =
            teams.computeIfAbsent(color) { V26_3TeamData(color) }

        fun getByColorOrNull(color: TeamColor): V26_3TeamData? = teams[color]

        fun removeFromAll(uuid: UUID) {
            teams.values.forEach { it.removeSeen(uuid) }
        }
    }
}
