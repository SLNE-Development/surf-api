package dev.slne.surf.api.minestom.dialog.builder

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.core.util.mutableObjectListOf
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import net.minestom.server.dialog.DialogInput
import org.jetbrains.annotations.Range

/**
 * Collects the input controls of a dialog.
 *
 * Every control is identified by a key, which is how its value is reported back with the click
 * payload once the dialog is submitted.
 */
class DialogInputBuilder {
    private val inputs = mutableObjectListOf<DialogInput>()

    fun input(input: DialogInput) {
        inputs.add(input)
    }

    fun boolean(key: String, block: BooleanInputBuilder.() -> Unit) {
        input(BooleanInputBuilder(key).apply(block).build())
    }

    fun text(key: String, block: TextInputBuilder.() -> Unit) {
        input(TextInputBuilder(key).apply(block).build())
    }

    fun numberRange(key: String, block: NumberRangeInputBuilder.() -> Unit) {
        input(NumberRangeInputBuilder(key).apply(block).build())
    }

    /**
     * Adds a number range control spanning [range].
     *
     * ```
     * numberRange("strength", 1.0..200.0) {
     *     label { info("Strength") }
     *     step(1f)
     * }
     * ```
     */
    fun <N> numberRange(
        key: String,
        range: ClosedRange<N>,
        block: NumberRangeInputBuilder.() -> Unit,
    ) where N : Number, N : Comparable<N> {
        numberRange(key) {
            range(range.start.toFloat(), range.endInclusive.toFloat())
            block()
        }
    }

    fun singleOption(key: String, block: SingleOptionInputBuilder.() -> Unit) {
        input(SingleOptionInputBuilder(key).apply(block).build())
    }

    internal fun build(): ObjectList<DialogInput> = inputs

    /**
     * The shared state of every input control: a key and a label.
     */
    abstract class InputBuilder(protected val key: String) {
        var label: Component? = null

        fun label(label: Component) {
            this.label = label
        }

        fun label(block: SurfComponentBuilder.() -> Unit) {
            label(SurfComponentBuilder(block))
        }

        protected fun requireLabel(): Component {
            val label = label
            require(label != null) { "Dialog input '$key' must have a label" }
            return label
        }
    }

    class BooleanInputBuilder(key: String) : InputBuilder(key) {
        var initial: Boolean = false
        var onTrue: String = "true"
        var onFalse: String = "false"

        fun initial(initial: Boolean) {
            this.initial = initial
        }

        fun onTrue(onTrue: String) {
            this.onTrue = onTrue
        }

        fun onFalse(onFalse: String) {
            this.onFalse = onFalse
        }

        internal fun build() = DialogInput.Boolean(key, requireLabel(), initial, onTrue, onFalse)
    }

    class TextInputBuilder(key: String) : InputBuilder(key) {
        var width: @Range(from = 1, to = 1024) Int = DialogInput.DEFAULT_WIDTH
        var labelVisible: Boolean = true
        var initial: String = ""
        var maxLength: Int = DEFAULT_MAX_LENGTH
        private var multiline: DialogInput.Text.Multiline? = null

        fun width(width: @Range(from = 1, to = 1024) Int) {
            this.width = width
        }

        fun labelVisible(visible: Boolean) {
            this.labelVisible = visible
        }

        fun initial(initial: String) {
            this.initial = initial
        }

        fun maxLength(maxLength: Int) {
            this.maxLength = maxLength
        }

        fun multiline(maxLines: Int? = null, height: Int? = null) {
            multiline = DialogInput.Text.Multiline(maxLines, height)
        }

        internal fun build() = DialogInput.Text(
            key,
            width,
            requireLabel(),
            labelVisible,
            initial,
            maxLength,
            multiline
        )

        private companion object {
            const val DEFAULT_MAX_LENGTH = 32
        }
    }

    class NumberRangeInputBuilder(key: String) : InputBuilder(key) {
        var width: @Range(from = 1, to = 1024) Int = DialogInput.DEFAULT_WIDTH
        var labelFormat: String = "options.generic_value"
        var start: Float? = null
        var end: Float? = null
        var initial: Float? = null
        var step: Float? = null

        fun width(width: @Range(from = 1, to = 1024) Int) {
            this.width = width
        }

        fun labelFormat(labelFormat: String) {
            this.labelFormat = labelFormat
        }

        fun range(start: Float, end: Float) {
            this.start = start
            this.end = end
        }

        fun initial(initial: Float) {
            this.initial = initial
        }

        fun step(step: Float) {
            this.step = step
        }

        internal fun build(): DialogInput.NumberRange {
            val start = start
            val end = end
            require(start != null && end != null) {
                "Dialog input '$key' must declare a range"
            }

            return DialogInput.NumberRange(
                key,
                width,
                requireLabel(),
                labelFormat,
                start,
                end,
                initial,
                step
            )
        }
    }

    class SingleOptionInputBuilder(key: String) : InputBuilder(key) {
        var width: @Range(from = 1, to = 1024) Int = DialogInput.DEFAULT_WIDTH
        var labelVisible: Boolean = true
        private val options = mutableObjectListOf<DialogInput.SingleOption.Option>()

        fun width(width: @Range(from = 1, to = 1024) Int) {
            this.width = width
        }

        fun labelVisible(visible: Boolean) {
            this.labelVisible = visible
        }

        fun option(id: String, display: Component? = null, initial: Boolean = false) {
            options.add(DialogInput.SingleOption.Option(id, display, initial))
        }

        fun option(
            id: String,
            initial: Boolean = false,
            display: SurfComponentBuilder.() -> Unit,
        ) {
            option(id, SurfComponentBuilder(display), initial)
        }

        internal fun build(): DialogInput.SingleOption {
            require(options.isNotEmpty()) { "Dialog input '$key' must offer at least one option" }

            return DialogInput.SingleOption(key, width, options, requireLabel(), labelVisible)
        }
    }
}

fun dialogInput(block: DialogInputBuilder.() -> Unit): ObjectList<DialogInput> =
    DialogInputBuilder().apply(block).build()
