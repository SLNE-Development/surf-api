package dev.slne.surf.api.paper.server.nms.v1_21_11.glow

import net.minecraft.ChatFormatting
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class V1_21_11TeamData(color: ChatFormatting) {
    private val scoreboard = Scoreboard()
    val teamId = "glow-${uid()}${color.char}"
    val team = PlayerTeam(scoreboard, teamId).apply {
        collisionRule = Team.CollisionRule.NEVER
        this.color = color
    }

    private val seenBy = ConcurrentHashMap.newKeySet<UUID>()

    fun markSeen(uniqueId: UUID) = seenBy.add(uniqueId)
    fun isSeen(uniqueId: UUID): Boolean = seenBy.contains(uniqueId)
    fun removeSeen(uniqueId: UUID) = seenBy.remove(uniqueId)

    companion object {
        private val lastUid = AtomicInteger()
        fun uid(): Int = lastUid.getAndIncrement()

        private val teams = EnumMap<ChatFormatting, V1_21_11TeamData>(ChatFormatting::class.java)

        fun getByColor(color: ChatFormatting): V1_21_11TeamData =
            teams.computeIfAbsent(color) { V1_21_11TeamData(color) }

        fun getByColorOrNull(color: ChatFormatting): V1_21_11TeamData? = teams[color]

        fun removeFromAll(uuid: UUID) {
            teams.values.forEach { it.removeSeen(uuid) }
        }
    }
}
