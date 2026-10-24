package dev.slne.surf.api.minestom.server.chat.impl.signature


internal fun interface SignatureUpdater {
    fun update(output: Output)

    fun interface Output {
        fun update(payload: ByteArray)
    }
}
