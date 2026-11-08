package dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory

import dev.slne.surf.api.core.messages.adventure.text
import dev.slne.surf.api.paper.builder.buildItem
import dev.slne.surf.api.paper.builder.displayName
import dev.slne.surf.api.paper.inventory.framework.dsl.slot
import dev.slne.surf.api.paper.inventory.framework.dsl.withItem
import dev.slne.surf.api.paper.inventory.framework.view.button.button
import dev.slne.surf.api.paper.inventory.framework.view.button.computedToggleButton
import dev.slne.surf.api.paper.inventory.framework.view.button.statefulButton
import dev.slne.surf.api.paper.inventory.framework.view.button.toggleButton
import dev.slne.surf.api.paper.inventory.framework.view.button.tripleButton
import dev.slne.surf.api.paper.inventory.framework.view.onFirstRender
import dev.slne.surf.api.paper.inventory.framework.view.surfView
import org.bukkit.inventory.ItemType

/** Sichtbarkeitsstufen für den Tripple-Button. */
enum class TestVisibility { ALL, FRIENDS, NONE }

/** Schwierigkeitsgrade für den Multi-State-Button. */
enum class TestDifficulty { EASY, NORMAL, HARD, INSANE }

/**
 * In-memory stand-in for whatever would normally persist a player's settings.
 */
object TestSettingsStore {
    private val sounds = mutableMapOf<String, Boolean>()

    fun soundEnabled(player: String) = sounds.getOrDefault(player, true)
    fun setSoundEnabled(player: String, enabled: Boolean) {
        sounds[player] = enabled
    }
}

val testButtonView = surfView("Einstellungen") {

    // An/Aus-Button mit fest vorgegebenem Startwert.
    val particles = toggleButton(initial = true) {
        whenOn {
            item(ItemType.LIME_DYE) { displayName { text("Partikel: an") } }
            onEnter { player.sendMessage(text("Partikel eingeschaltet")) }
        }
        whenOff {
            item(ItemType.GRAY_DYE) { displayName { text("Partikel: aus") } }
        }

        onToggle { enabled -> player.sendMessage(text("Partikel: $enabled")) }
        onCloseChanged { initial, current ->
            player.sendMessage(text("Partikel von $initial auf $current geändert"))
        }
    }

    // An/Aus-Button, dessen Startwert pro Spieler geladen wird.
    val sound = computedToggleButton({ context ->
        TestSettingsStore.soundEnabled(context.player.name)
    }) {
        whenOn { item(ItemType.NOTE_BLOCK) { displayName { text("Sound: an") } } }
        whenOff { item(ItemType.BARRIER) { displayName { text("Sound: aus") } } }

        onCloseChanged { _, current ->
            TestSettingsStore.setSoundEnabled(player.name, current)
            player.sendMessage(text("Sound gespeichert: $current"))
        }
    }

    // Tripple-Button: genau drei Stufen.
    val visibility = tripleButton(TestVisibility.ALL) {
        state(TestVisibility.ALL) {
            item(ItemType.LIME_DYE) { displayName { text("Sichtbar für alle") } }
        }
        state(TestVisibility.FRIENDS) {
            item(ItemType.YELLOW_DYE) { displayName { text("Sichtbar für Freunde") } }
        }
        state(TestVisibility.NONE) {
            item(ItemType.RED_DYE) { displayName { text("Für niemanden sichtbar") } }
        }

        onCloseChanged { initial, current ->
            player.sendMessage(text("Sichtbarkeit: $initial -> $current"))
        }
    }

    // Beliebig viele Stufen, Rechtsklick rückwärts ist hier abgeschaltet.
    val difficulty = statefulButton(TestDifficulty.NORMAL) {
        reverseOnRightClick(false)

        state(TestDifficulty.EASY) { item(ItemType.WHITE_WOOL) { displayName { text("Einfach") } } }
        state(TestDifficulty.NORMAL) { item(ItemType.YELLOW_WOOL) { displayName { text("Normal") } } }
        state(TestDifficulty.HARD) { item(ItemType.ORANGE_WOOL) { displayName { text("Schwer") } } }
        state(TestDifficulty.INSANE) {
            // Vollständig dynamisches Aussehen.
            renderItem {
                buildItem(ItemType.RED_WOOL, 1) {
                    displayName { text("Wahnsinn (${player.name})") }
                }
            }
        }

        onChange { from, to -> player.sendMessage(text("Schwierigkeit $from -> $to")) }
        onCloseChanged { _, current -> player.sendMessage(text("Gespeichert: $current")) }
    }

    onFirstRender {
        slot(1, 1) { button(particles) }
        slot(1, 3) { button(sound) }
        slot(1, 5) { button(visibility) }
        slot(1, 7) { button(difficulty) }

        // Buttonwerte lassen sich aus jedem Context lesen.
        slot(3, 4) {
            withItem(ItemType.PAPER) { displayName { text("Übersicht") } }
            onClick { click ->
                click.player.sendMessage(
                    text(
                        "Partikel=${particles[click]}, Sound=${sound[click]}, " +
                                "Sicht=${visibility[click]}, Schwierigkeit=${difficulty[click]}"
                    )
                )
            }
        }
    }
}
