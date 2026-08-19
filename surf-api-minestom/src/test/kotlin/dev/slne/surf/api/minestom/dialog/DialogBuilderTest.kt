package dev.slne.surf.api.minestom.dialog

import dev.slne.surf.api.minestom.dialog.builder.dialogAction
import dev.slne.surf.api.minestom.dialog.callback.DialogCallbackOptions
import dev.slne.surf.api.minestom.dialog.callback.DialogCallbacks
import dev.slne.surf.api.minestom.dialog.callback.DialogResponseView
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Pos
import net.minestom.server.dialog.*
import net.minestom.server.entity.Player
import net.minestom.server.registry.HolderSet
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
    fun `a dialog list carries its dialogs and its layout`() {
        val first = noticeDialog(Component.text("First"), Component.text("One"))
        val second = noticeDialog(Component.text("Second"), Component.text("Two"))

        val dialog = dialog {
            base { title { primary("List") } }
            type {
                dialogList(listOf(first, second)) {
                    columns(3)
                    buttonWidth(200)
                    exitAction { label(Component.text("Back")) }
                }
            }
        }

        val list = assertIs<Dialog.DialogList>(dialog)
        assertEquals(3, list.columns())
        assertEquals(200, list.buttonWidth())
        assertEquals(Component.text("Back"), list.exitAction()?.label())

        val dialogs = assertIs<HolderSet.Direct<Dialog>>(list.dialogs())
        assertEquals(listOf(first, second), dialogs.values())
    }

    @Test
    fun `a dialog list offers every dialog only once`() {
        val entry = noticeDialog(Component.text("Only"), Component.text("One"))

        val dialog = dialog {
            base { title { primary("List") } }
            type { dialogList(entry, entry) }
        }

        val list = assertIs<Dialog.DialogList>(dialog)
        val dialogs = assertIs<HolderSet.Direct<Dialog>>(list.dialogs())
        assertEquals(listOf(entry), dialogs.values())
    }

    @Test
    fun `a server links dialog falls back to the vanilla layout`() {
        val dialog = dialog {
            base { title { primary("Links") } }
            type { serverLinks() }
        }

        val links = assertIs<Dialog.ServerLinks>(dialog)
        assertEquals(2, links.columns())
        assertEquals(150, links.buttonWidth())
        assertNull(links.exitAction())
    }

    @Test
    fun `a number range spans the range it was declared with`() {
        val dialog = noticeDialog {
            base {
                title { primary("Range") }
                input {
                    numberRange("strength", 1.0..200.0) {
                        label { info("Strength") }
                        step(1f)
                    }
                }
            }
        }

        val range = assertIs<DialogInput.NumberRange>(dialog.metadata().inputs()[0])
        assertEquals(1f, range.start())
        assertEquals(200f, range.end())
        assertEquals(1f, range.step())
    }

    @Test
    fun `a custom click button asks the client for its input values`() {
        val dialog = dialog {
            base { title { primary("Custom") } }
            type {
                notice {
                    label { success("Send") }
                    action { customPlayerClick { _, _ -> } }
                }
            }
        }

        val notice = assertIs<Dialog.Notice>(dialog)
        val action = assertIs<DialogAction.DynamicCustom>(notice.action().action())
        assertTrue(DialogCallbacks.unregister(action.key()))
    }

    @Test
    fun `a custom click callback reads the values the client reported`(env: Env) {
        val player = env.createPlayer()
        var seen: DialogResponseView? = null

        val action = assertIs<DialogAction.DynamicCustom>(
            dialogAction { customPlayerClick { response, _ -> seen = response } }
        )

        val payload = CompoundBinaryTag.builder()
            .putString("message", "hello")
            .putFloat("strength", 12.5f)
            .putString("silent", "true")
            .build()

        assertTrue(DialogCallbacks.dispatch(player, action.key(), payload))

        val response = assertNotNull(seen)
        assertEquals("hello", response.getText("message"))
        assertEquals(12.5f, response.getFloat("strength"))
        assertEquals(true, response.getBoolean("silent"))
        assertNull(response.getText("absent"))
    }

    @Test
    fun `a custom click callback without values reads nothing`(env: Env) {
        var seen: DialogResponseView? = null

        val action = assertIs<DialogAction.DynamicCustom>(
            dialogAction { customPlayerClick { response, _ -> seen = response } }
        )

        assertTrue(DialogCallbacks.dispatch(env.createPlayer(), action.key(), null))

        assertEquals(DialogResponseView.EMPTY, assertNotNull(seen))
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
