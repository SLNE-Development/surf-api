package dev.slne.surf.api.minestom.dialog.callback

import net.kyori.adventure.nbt.*

/**
 * The values a dialog collected, as the client reported them back.
 *
 * A control is read by the key it was declared under. Reading a key the dialog does not have, or
 * reading it as the wrong kind of value, yields `null` rather than failing.
 *
 * @property payload the raw values the client sent
 */
@JvmInline
value class DialogResponseView(val payload: CompoundBinaryTag) {

    /**
     * Returns the text a text control holds.
     *
     * @param key the key the control was declared under
     * @return the text, or `null` if the dialog holds no text under that key
     */
    fun getText(key: String): String? = (payload[key] as? StringBinaryTag)?.value()

    /**
     * Returns the number a number range control holds.
     *
     * @param key the key the control was declared under
     * @return the number, or `null` if the dialog holds no number under that key
     */
    fun getFloat(key: String): Float? = (payload[key] as? NumberBinaryTag)?.floatValue()

    /**
     * Returns whether a boolean control is switched on.
     *
     * Boolean controls report themselves as the text they were configured to submit, so a control
     * with custom texts is only understood when those texts read as booleans.
     *
     * @param key the key the control was declared under
     * @return whether the control is on, or `null` if the dialog holds no boolean under that key
     */
    fun getBoolean(key: String): Boolean? = when (val tag: BinaryTag? = payload[key]) {
        is ByteBinaryTag -> tag.value() != ZERO
        is StringBinaryTag -> tag.value().toBooleanStrictOrNull()
        else -> null
    }

    companion object {
        private const val ZERO: Byte = 0

        /**
         * The empty response, standing in for a dialog that reported no values at all.
         */
        val EMPTY = DialogResponseView(CompoundBinaryTag.empty())

        /**
         * Reads [payload] as the values of a dialog.
         *
         * @param payload what the client sent along with the click
         * @return the values, or [EMPTY] if the client sent something else
         */
        fun of(payload: BinaryTag?): DialogResponseView =
            if (payload is CompoundBinaryTag) DialogResponseView(payload) else EMPTY
    }
}
