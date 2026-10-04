package dev.slne.surf.api.minestom.server.visibility

import dev.slne.surf.api.minestom.visibility.PlayerVisibility
import net.minestom.server.entity.AutomaticPlayerVisibility
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate

/**
 * The visibility state of one viewing player.
 *
 * The player's viewer rule is owned by this handler: it combines the rule requested through
 * [PlayerVisibility.updateViewerRule] with the budget of nearest players the
 * [PlayerVisibilityService] selected.
 */
internal class PlayerVisibilityHandler(private val player: Player) {
    private class RuleRequest(
        val predicate: Predicate<in Entity>?,
    )

    @Suppress("ArrayInDataClass")
    data class VisibilitySelection(
        val instance: Instance?,
        val ids: IntArray,
    )

    companion object {
        val EMPTY_IDS = IntArray(0)
    }

    private val requestedRule = AtomicReference(RuleRequest(null))
    private var appliedRequest = requestedRule.get()

    @Volatile
    private var appliedRule: Predicate<in Entity>? = null

    @Volatile
    private var admittingPlayers = true

    @Volatile
    var visibilitySelection = VisibilitySelection(null, EMPTY_IDS)
        private set

    var visibilityLimited = false

    var requiresNativeRuleRefresh = false
        private set

    private val combinedRule = Predicate<Entity> { entity ->
        if (!allowsWithoutBudget(entity)) {
            false
        } else if (entity !is Player) {
            true
        } else {
            val selection = visibilitySelection

            admittingPlayers &&
                    player.autoViewEntities() &&
                    selection.instance === player.instance &&
                    entity.instance === player.instance &&
                    selection.ids.binarySearch(entity.entityId) >= 0
        }
    }

    fun init() {
        player.updateViewerRule(combinedRule)

        // A viewable rule makes Minestom send this player's packets to its actual viewers only,
        // rather than to every player in the surrounding chunks - most of whom no longer see it.
        player.updateViewableRule(ALWAYS_VIEWABLE)
    }

    fun close() {
        player.updateViewerRule(appliedRule)
        player.updateViewableRule(null)
    }

    fun requestRule(predicate: Predicate<in Entity>?) {
        requestedRule.set(RuleRequest(predicate))
    }

    fun requestRuleRefresh() {
        requestedRule.updateAndGet { RuleRequest(it.predicate) }
    }

    fun allowsWithoutBudget(entity: Entity): Boolean {
        return appliedRule?.test(entity) != false
    }

    fun applyRequestedRule(): Boolean {
        val request = requestedRule.get()
        if (request === appliedRequest) return false

        appliedRequest = request
        appliedRule = request.predicate
        requiresNativeRuleRefresh = true

        return true
    }

    fun pausePlayerAdmission() {
        AutomaticPlayerVisibility.withViewerLock(player) {
            admittingPlayers = false
        }
    }

    fun publishSelection(instance: Instance, ids: IntArray) {
        val previous = visibilitySelection

        if (previous.instance !== instance || previous.ids !== ids) {
            visibilitySelection = VisibilitySelection(instance, ids)
        }
    }

    fun resumePlayerAdmission() {
        AutomaticPlayerVisibility.withViewerLock(player) {
            admittingPlayers = true
        }
    }

    fun refreshNativeViewerRule() {
        if (player.autoViewEntities()) {
            player.updateViewerRule()
        }

        requiresNativeRuleRefresh = false
    }
}

private val ALWAYS_VIEWABLE = Predicate<Player> { true }
