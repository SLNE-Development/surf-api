package dev.slne.surf.api.minestom.dialog.builder

import net.minestom.server.dialog.Dialog

/**
 * Collects the two halves of a dialog until they can be assembled.
 */
class DialogEntryBuilder {
    private var base: DialogBaseBuilder? = null
    private var type: DialogTypeBuilder? = null

    fun base(base: DialogBaseBuilder) {
        this.base = base
    }

    fun type(type: DialogTypeBuilder) {
        this.type = type
    }

    internal fun build(): Dialog {
        val base = base
        val type = type
        require(base != null) { "Dialog base must be built" }
        require(type != null) { "Dialog type must be built" }

        return type.build(base.build())
    }
}
