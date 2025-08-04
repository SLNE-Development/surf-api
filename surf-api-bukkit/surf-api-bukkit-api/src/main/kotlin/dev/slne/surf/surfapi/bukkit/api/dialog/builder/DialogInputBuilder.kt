@file:Suppress("UnstableApiUsage")

package dev.slne.surf.surfapi.bukkit.api.dialog.builder

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput.OptionEntry
import io.papermc.paper.registry.data.dialog.input.TextDialogInput.MultilineOptions
import it.unimi.dsi.fastutil.objects.ObjectList
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.Range

class DialogInputBuilder {
    private val inputs = mutableObjectListOf<DialogInput>()

    fun text(key: String, block: TextDialogInput.() -> Unit): DialogInput {
        val input = TextDialogInput(key).apply(block).build()
        inputs.add(input)
        return input
    }

    fun simpleText(key: String, label: Component) {
        val input = DialogInput.text(key, label)
        inputs.add(input.build())
    }

    fun simpleText(key: String, block: SurfComponentBuilder.() -> Unit) {
        simpleText(key, SurfComponentBuilder(block))
    }

    fun boolean(key: String, block: BooleanDialogInput.() -> Unit): DialogInput {
        val input = BooleanDialogInput(key).apply(block).build()
        inputs.add(input)
        return input
    }

    fun simpleBoolean(key: String, label: Component, defaultValue: Boolean? = null) {
        val input = DialogInput.bool(key, label)
        defaultValue?.let { input.initial(it) }
        inputs.add(input.build())
    }

    fun simpleBoolean(
        key: String,
        defaultValue: Boolean? = null,
        block: SurfComponentBuilder.() -> Unit,
    ) {
        simpleBoolean(key, SurfComponentBuilder(block), defaultValue)
    }

    fun <N> numberRange(
        key: String,
        range: ClosedRange<N>,
        block: NumberRangeDialogInput.() -> Unit,
    ): DialogInput where N : Number, N : Comparable<N> {
        val input =
            NumberRangeDialogInput(key, range.start.toFloat(), range.endInclusive.toFloat()).apply(
                block
            ).build()
        inputs.add(input)
        return input
    }

    fun <N> numberRange(
        key: String,
        range: OpenEndRange<N>,
        block: NumberRangeDialogInput.() -> Unit,
    ): DialogInput where N : Number, N : Comparable<N> {
        val input = NumberRangeDialogInput(
            key,
            range.start.toFloat(),
            (range.endExclusive.toLong() - 1).toFloat()
        ).apply(block).build()
        inputs.add(input)
        return input
    }

    fun <N> simpleNumberRange(
        key: String,
        range: ClosedRange<N>,
        label: Component,
    ) where N : Number, N : Comparable<N> {
        val input =
            DialogInput.numberRange(key, label, range.start.toFloat(), range.endInclusive.toFloat())
        inputs.add(input.build())
    }

    fun <N> simpleNumberRange(
        key: String,
        range: ClosedRange<N>,
        block: SurfComponentBuilder.() -> Unit,
    ) where N : Number, N : Comparable<N> {
        simpleNumberRange(key, range, SurfComponentBuilder(block))
    }

    fun <N> simpleNumberRange(
        key: String,
        range: OpenEndRange<N>,
        label: Component,
    ) where N : Number, N : Comparable<N> {
        val input =
            DialogInput.numberRange(
                key,
                label,
                range.start.toFloat(),
                (range.endExclusive.toLong() - 1).toFloat()
            )
        inputs.add(input.build())
    }

    fun <N> simpleNumberRange(
        key: String,
        range: OpenEndRange<N>,
        block: SurfComponentBuilder.() -> Unit,
    ) where N : Number, N : Comparable<N> {
        simpleNumberRange(key, range, SurfComponentBuilder(block))
    }

    fun singleOption(
        key: String,
        block: DialogInputBuilder.SingleOptionDialogInput.() -> Unit,
    ): DialogInput {
        val input = SingleOptionDialogInput(key).apply(block).build()
        inputs.add(input)
        return input
    }

    fun addInput(input: DialogInput) {
        inputs.add(input)
    }

    internal fun build(): ObjectList<DialogInput> {
        return inputs
    }

    class TextDialogInput(private val key: String) {
        var label: Component? = null
        var width: @Range(from = 1, to = 1024) Int? = null
        var labelVisible: Boolean? = null
        var initial: String? = null
        var maxLength: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int? = null
        var multilineOptions: MultilineOptions? = null

        fun label(label: Component) {
            this.label = label
        }

        fun label(block: SurfComponentBuilder.() -> Unit) {
            label(SurfComponentBuilder(block))
        }

        fun width(width: @Range(from = 1, to = 1024) Int) {
            this.width = width
        }

        fun labelVisible(labelVisible: Boolean) {
            this.labelVisible = labelVisible
        }

        fun initial(initial: String) {
            this.initial = initial
        }

        fun maxLength(maxLength: @Range(from = 1, to = Int.MAX_VALUE.toLong()) Int) {
            this.maxLength = maxLength
        }

        fun multiline(multilineOptions: MultilineOptions) {
            this.multilineOptions = multilineOptions
        }

        fun multiline(
            maxLines: Int? = null,
            height: Int? = null,
        ) {
            this.multilineOptions = MultilineOptions.create(maxLines, height)
        }

        internal fun build(): DialogInput {
            val label = label
            require(label != null) { "Dialog input label must not be null" }
            val builder = DialogInput.text(key, label)
            with(builder) {
                width?.let { width(it) }
                labelVisible?.let { labelVisible(it) }
                initial?.let { initial(it) }
                maxLength?.let { maxLength(it) }
                multilineOptions?.let { multiline(it) }
            }
            return builder.build()
        }
    }

    class BooleanDialogInput(private val key: String) {
        var label: Component? = null
        var initial: Boolean? = null
        var onTrue: String? = null
        var onFalse: String? = null

        fun label(label: Component) {
            this.label = label
        }

        fun label(block: SurfComponentBuilder.() -> Unit) {
            label(SurfComponentBuilder(block))
        }

        fun initial(initial: Boolean) {
            this.initial = initial
        }

        fun onTrue(onTrue: String) {
            this.onTrue = onTrue
        }

        fun onFalse(onFalse: String) {
            this.onFalse = onFalse
        }

        internal fun build(): DialogInput {
            val label = label
            require(label != null) { "Dialog input label must not be null" }
            val builder = DialogInput.bool(key, label)
            with(builder) {
                initial?.let { initial(it) }
                onTrue?.let { onTrue(it) }
                onFalse?.let { onFalse(it) }
            }
            return builder.build()
        }
    }

    class NumberRangeDialogInput(
        private val key: String,
        private val start: Float,
        private val end: Float,
    ) {
        var label: Component? = null
        var initial: Float? = null
        var width: @Range(from = 1, to = 1024) Int? = null
        var labelFormat: String? = null
        var step: Float? = null

        fun label(label: Component) {
            this.label = label
        }

        fun label(block: SurfComponentBuilder.() -> Unit) {
            label(SurfComponentBuilder(block))
        }

        fun initial(initial: Float) {
            this.initial = initial
        }

        fun width(width: @Range(from = 1, to = 1024) Int) {
            this.width = width
        }

        fun labelFormat(labelFormat: String) {
            this.labelFormat = labelFormat
        }

        fun step(step: Float) {
            this.step = step
        }

        internal fun build(): DialogInput {
            val label = label
            require(label != null) { "Dialog input label must not be null" }
            val builder = DialogInput.numberRange(
                key,
                label,
                start,
                end
            )
            with(builder) {
                initial?.let { initial(it) }
                width?.let { width(it) }
                labelFormat?.let { labelFormat(it) }
                step?.let { step(it) }
            }
            return builder.build()
        }
    }

    class SingleOptionDialogInput(private val key: String) {
        var label: Component? = null
        val entries = mutableObjectListOf<OptionEntry>()
        var width: @Range(from = 1, to = 1024) Int? = null
        var labelVisible: Boolean? = null

        fun label(label: Component) {
            this.label = label
        }

        fun label(block: SurfComponentBuilder.() -> Unit) {
            label(SurfComponentBuilder(block))
        }

        fun option(entry: OptionEntry) {
            entries.add(entry)
        }

        fun option(
            key: String,
            label: Component,
            selected: Boolean = false,
        ) {
            entries.add(OptionEntry.create(key, label, selected))
        }

        fun option(
            key: String,
            selected: Boolean = false,
            block: SurfComponentBuilder.() -> Unit,
        ) {
            option(key, SurfComponentBuilder(block), selected)
        }

        internal fun build(): DialogInput {
            val label = label
            require(label != null) { "Dialog input label must not be null" }
            val builder = DialogInput.singleOption(key, label, entries)
            with(builder) {
                width?.let { width(it) }
                labelVisible?.let { labelVisible(it) }
            }
            return builder.build()
        }
    }

}

fun DialogInput(block: DialogInputBuilder.() -> Unit): ObjectList<DialogInput> =
    DialogInputBuilder().apply(block).build()

fun dialogInput(block: DialogInputBuilder.() -> Unit): ObjectList<DialogInput> =
    DialogInputBuilder().apply(block).build()