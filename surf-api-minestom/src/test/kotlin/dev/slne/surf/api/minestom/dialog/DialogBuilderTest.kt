package dev.slne.surf.api.minestom.dialog

import dev.slne.surf.api.minestom.dialog.callback.DialogCallbackOptions
import dev.slne.surf.api.minestom.dialog.callback.DialogCallbacks
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.dialog.*
import net.minestom.server.entity.Player
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Test
import kotlin.test.*

@EnvTest
class DialogBuilderTest {

    @Test
    fun `notice dialog carries its title and body`() {
        val dialog = noticeDialog(Component.text("Title"), Component.text("Notice"))

        val notice = assertIs<Dialog.Notice>(dialog)
        assertEquals(Component.text("Title"), notice.metadata().title())
        assertEquals(
            listOf(DialogBody.PlainMessage(Component.text("Notice"), 200)),
            notice.metadata().body()
        )
    }

    @Test
    fun `a dialog does not pause the game by default`() {
        val dialog = noticeDialog(Component.text("Title"), Component.text("Notice"))

        assertFalse(dialog.metadata().pause())
        assertTrue(dialog.metadata().canCloseWithEscape())
        assertEquals(DialogAfterAction.CLOSE, dialog.metadata().afterAction())
    }

    @Test
    fun `a confirmation dialog keeps its base and both buttons`() {
        val dialog = dialog {
            base {
                title { primary("Confirm") }
                preventClosingWithEscape()
                afterAction(DialogAfterAction.WAIT_FOR_RESPONSE)
                body {
                    plainMessage { info("Body") }
                }
            }
            type {
                confirmation {
                    yes {
                        label { success("Yes") }
                        action { runCommand("say yes") }
                    }
                    no {
                        label { error("No") }
                    }
                }
            }
        }

        val confirmation = assertIs<Dialog.Confirmation>(dialog)
        assertFalse(confirmation.metadata().canCloseWithEscape())
        assertEquals(DialogAfterAction.WAIT_FOR_RESPONSE, confirmation.metadata().afterAction())
        assertEquals(DialogAction.RunCommand("say yes"), confirmation.yesButton().action())
        assertNull(confirmation.noButton().action())
    }

    @Test
    fun `inputs are declared in the order they are added`() {
        val dialog = noticeDialog {
            base {
                title { primary("Inputs") }
                input {
                    text("message") {
                        label { info("Message") }
                        maxLength(64)
                    }
                    boolean("silent") {
                        label { info("Silent") }
                        initial(true)
                    }
                }
            }
        }

        val inputs = dialog.metadata().inputs()
        assertEquals(2, inputs.size)

        val text = assertIs<DialogInput.Text>(inputs[0])
        assertEquals("message", text.key())
        assertEquals(64, text.maxLength())

        val boolean = assertIs<DialogInput.Boolean>(inputs[1])
        assertEquals("silent", boolean.key())
        assertTrue(boolean.initial())
    }

    @Test
    fun `a multi action dialog rejects having no action`() {
        val failure = runCatching {
            dialog {
                base { title { primary("Empty") } }
                type { multiAction { } }
            }
        }.exceptionOrNull()

        assertIs<IllegalArgumentException>(failure)
    }

    @Test
    fun `a callback button reports a custom action key`() {
        val dialog = dialog {
            base { title { primary("Callback") } }
            type {
                notice {
                    label { success("Run") }
                    action { playerCallback { } }
                }
            }
        }

        val notice = assertIs<Dialog.Notice>(dialog)
        val action = assertIs<DialogAction.Custom>(notice.action().action())
        assertTrue(DialogCallbacks.unregister(action.key()))
        assertFalse(DialogCallbacks.unregister(action.key()))
    }

    @Test
    fun `a single use callback runs once`(env: Env) {
        val player = env.createPlayer()
        var pressedBy: Player? = null
        val key = DialogCallbacks.register { pressedBy = it.player }

        assertTrue(DialogCallbacks.dispatch(player, key, null))
        assertFalse(DialogCallbacks.dispatch(player, key, null))
        assertEquals(player, pressedBy)
    }

    @Test
    fun `an unlimited callback keeps running`(env: Env) {
        val player = env.createPlayer()
        var runs = 0
        val key = DialogCallbacks.register(
            DialogCallbackOptions(uses = DialogCallbackOptions.UNLIMITED_USES)
        ) { runs++ }

        repeat(3) { assertTrue(DialogCallbacks.dispatch(player, key, null)) }
        assertEquals(3, runs)

        assertTrue(DialogCallbacks.unregister(key))
    }

    @Test
    fun `an unknown key runs nothing`(env: Env) {
        assertFalse(DialogCallbacks.dispatch(env.createPlayer(), Key.key("surf", "absent"), null))
    }

    private fun Env.createPlayer(): Player = createPlayer(createFlatInstance(), Pos.ZERO)
}
