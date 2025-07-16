package dev.slne.surf.surfapi.bukkit.server.impl.glow

import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import net.minecraft.ChatFormatting
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class TeamData(color: ChatFormatting) {
    private val scoreboard = Scoreboard()
    val teamId = "glow-${uid()}${color.char}"
    val team = PlayerTeam(scoreboard, teamId).apply {
        collisionRule = Team.CollisionRule.NEVER
        this.color = color
    }

    private val seenBy = mutableObjectSetOf<UUID>()

    fun markSeen(uniqueId: UUID) = seenBy.add(uniqueId)
    fun isSeen(uniqueId: UUID): Boolean = seenBy.contains(uniqueId)
    fun removeSeen(uniqueId: UUID) = seenBy.remove(uniqueId)

    companion object {
        private val lastUid = AtomicInteger()
        fun uid(): Int = lastUid.getAndIncrement()

        private val teams = EnumMap<ChatFormatting, TeamData>(ChatFormatting::class.java)

        fun getByColor(color: ChatFormatting): TeamData =
            teams.computeIfAbsent(color) { TeamData(color) }

        fun getByColorOrNull(color: ChatFormatting): TeamData? = teams[color]

        fun removeFromAll(uuid: UUID) {
            teams.values.forEach { it.removeSeen(uuid) }
        }
    }
}