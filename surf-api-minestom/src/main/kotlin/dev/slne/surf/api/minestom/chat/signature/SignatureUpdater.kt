package dev.slne.surf.api.minestom.chat.signature


fun interface SignatureUpdater {
    fun update(output: Output)

    fun interface Output {
        fun update(payload: ByteArray)
    }
}
