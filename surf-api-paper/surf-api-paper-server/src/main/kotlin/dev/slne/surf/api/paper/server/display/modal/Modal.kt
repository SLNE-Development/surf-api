package dev.slne.surf.api.paper.server.display.modal

import dev.slne.surf.api.paper.display.color
import dev.slne.surf.api.paper.display.cursor.CursorStyle
import dev.slne.surf.api.paper.server.display.Display
import dev.slne.surf.api.paper.display.element.Div
import dev.slne.surf.api.paper.display.shape.Shape
import dev.slne.surf.api.paper.display.style.*

private val MODAL_OVERLAY = color(0, 0, 0, 160)
private val MODAL_BG = color(0x1E1E2E)
private val MODAL_SURFACE = color(0x313244)
private val MODAL_BORDER = color(0x585B70)
private val MODAL_TEXT = color(0xCDD6F4)
private val MODAL_SUBTEXT = color(0x9399B2)
private val MODAL_GREEN = color(0xA6E3A1)
private val MODAL_RED = color(0xF38BA8)
private val MODAL_BLUE = color(0x89B4FA)
private val MODAL_YELLOW = color(0xF9E2AF)

fun Display.confirmDialog(
    title: String,
    message: String,
    confirmText: String = "Bestätigen",
    cancelText: String = "Abbrechen",
    confirmColor: Int = MODAL_GREEN,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = { dismissModal() }
): Div {
    val display = this
    return Div().apply {
        style {
            backgroundColor = MODAL_OVERLAY
            justifyContent = JustifyContent.CENTER
            alignItems = AlignItems.CENTER
        }

        div {
            style {
                width = 320
                backgroundColor = MODAL_BG
                border = Border(2, MODAL_BORDER)
                padding = Insets(12, 16, 12, 16)
                gap = 10
            }

            div {
                style {
                    backgroundColor = MODAL_SURFACE
                    padding = Insets(6, 8, 6, 8)
                }
                label(title) {
                    style { color = MODAL_TEXT; fontSize = 16 }
                }
            }

            label(message) {
                style { color = MODAL_SUBTEXT; fontSize = 13 }
            }

            div {
                style {
                    flexDirection = FlexDirection.ROW
                    gap = 8
                    justifyContent = JustifyContent.CENTER
                }

                div {
                    style {
                        backgroundColor = MODAL_SURFACE
                        padding = Insets(4, 12, 4, 12)
                        border = Border(1, confirmColor)
                        cursor = CursorStyle.POINTER
                    }
                    label(confirmText) {
                        style { color = confirmColor; fontSize = 13 }
                    }
                    hoverable(
                        onEnter = { _ ->
                            style.backgroundColor = color(0x45475A)
                            display.update()
                        },
                        onExit = { _ ->
                            style.backgroundColor = MODAL_SURFACE
                            display.update()
                        }
                    )
                    onClick { _ -> onConfirm() }
                }

                div {
                    style {
                        backgroundColor = MODAL_SURFACE
                        padding = Insets(4, 12, 4, 12)
                        border = Border(1, MODAL_RED)
                        cursor = CursorStyle.POINTER
                    }
                    label(cancelText) {
                        style { color = MODAL_RED; fontSize = 13 }
                    }
                    hoverable(
                        onEnter = { _ ->
                            style.backgroundColor = color(0x45475A)
                            display.update()
                        },
                        onExit = { _ ->
                            style.backgroundColor = MODAL_SURFACE
                            display.update()
                        }
                    )
                    onClick { _ -> onCancel() }
                }
            }
        }
    }
}

fun Display.alertDialog(
    title: String,
    message: String,
    buttonText: String = "OK",
    buttonColor: Int = MODAL_BLUE,
    onDismiss: () -> Unit = { dismissModal() }
): Div {
    val display = this
    return Div().apply {
        style {
            backgroundColor = MODAL_OVERLAY
            justifyContent = JustifyContent.CENTER
            alignItems = AlignItems.CENTER
        }

        div {
            style {
                width = 300
                backgroundColor = MODAL_BG
                border = Border(2, MODAL_BORDER)
                padding = Insets(12, 16, 12, 16)
                gap = 10
            }

            div {
                style {
                    backgroundColor = MODAL_SURFACE
                    padding = Insets(6, 8, 6, 8)
                }
                label(title) {
                    style { color = MODAL_TEXT; fontSize = 16 }
                }
            }

            label(message) {
                style { color = MODAL_SUBTEXT; fontSize = 13 }
            }

            div {
                style {
                    alignItems = AlignItems.CENTER
                }
                div {
                    style {
                        backgroundColor = MODAL_SURFACE
                        padding = Insets(4, 16, 4, 16)
                        border = Border(1, buttonColor)
                        cursor = CursorStyle.POINTER
                    }
                    label(buttonText) {
                        style { color = buttonColor; fontSize = 13 }
                    }
                    hoverable(
                        onEnter = { _ ->
                            style.backgroundColor = color(0x45475A)
                            display.update()
                        },
                        onExit = { _ ->
                            style.backgroundColor = MODAL_SURFACE
                            display.update()
                        }
                    )
                    onClick { _ -> onDismiss() }
                }
            }
        }
    }
}

fun Display.successDialog(
    title: String = "Erfolg!",
    message: String,
    onDismiss: () -> Unit = { dismissModal() }
): Div = alertDialog(title, message, "OK", MODAL_GREEN, onDismiss)

fun Display.errorDialog(
    title: String = "Fehler",
    message: String,
    onDismiss: () -> Unit = { dismissModal() }
): Div = alertDialog(title, message, "OK", MODAL_RED, onDismiss)

fun Display.warningDialog(
    title: String = "Warnung",
    message: String,
    onDismiss: () -> Unit = { dismissModal() }
): Div = alertDialog(title, message, "OK", MODAL_YELLOW, onDismiss)
