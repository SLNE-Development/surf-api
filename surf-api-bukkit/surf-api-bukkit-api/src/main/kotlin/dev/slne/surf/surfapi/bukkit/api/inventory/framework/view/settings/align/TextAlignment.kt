package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.align

enum class TextAlignment {
    LEFT {
        override fun calculateShift(text: String, options: TextAlignmentOptions): Int = options.leftShift + options.padding
    },
    RIGHT {
        override fun calculateShift(
            text: String,
            options: TextAlignmentOptions
        ): Int {
            val usableWidth = options.containerWidth - (options.padding * 2)
            val freeSpace = usableWidth - calculateTextWidth(text, options)
            return options.leftShift + freeSpace + 1 + options.padding
        }
    },
    CENTER {
        override fun calculateShift(
            text: String,
            options: TextAlignmentOptions
        ): Int {
            val usableWidth = options.containerWidth - (options.padding * 2)
            val freeSpace = usableWidth - calculateTextWidth(text, options)
            return options.leftShift + (freeSpace / 2) + options.padding
        }
    };

    abstract fun calculateShift(text: String, options: TextAlignmentOptions): Int

    companion object {
        fun calculateTextWidth(text: String, options: TextAlignmentOptions): Int {
            if (text.isEmpty()) return 0
            val n = text.length

            return (n * options.charSize) + ((n - 1) * options.charSpacing)
        }
    }
}