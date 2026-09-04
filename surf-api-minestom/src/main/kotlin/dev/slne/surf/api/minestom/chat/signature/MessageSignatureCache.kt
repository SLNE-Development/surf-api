package dev.slne.surf.api.minestom.chat.signature

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minestom.server.crypto.MessageSignature


class MessageSignatureCache(capacity: Int = DEFAULT_CAPACITY) {

    private class Entry(val signature: MessageSignature) {
        val hash = signature.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entry) return false

            if (hash != other.hash) return false
            if (signature != other.signature) return false

            return true
        }

        override fun hashCode(): Int {
            return hash
        }
    }

    private val entries = arrayOfNulls<Entry>(capacity)

    fun pack(signature: MessageSignature): Int {
        val hash = signature.hashCode()

        for (index in entries.indices) {
            val entry = entries[index] ?: continue
            if (entry.hash == hash && entry.signature == signature) return index
        }

        return NOT_FOUND
    }

    fun unpack(id: Int): MessageSignature? = entries[id]?.signature

    fun push(body: SignedMessageBody, signature: MessageSignature?) {
        val lastSeen = body.lastSeen.entries
        val queue = ArrayDeque<Entry>(lastSeen.size + 1)
        lastSeen.forEach { entry -> queue.addLast(Entry(entry)) }

        if (signature != null) {
            queue.addLast(Entry(signature))
        }

        push(queue)
    }

    private fun push(queue: ArrayDeque<Entry>) {
        val newEntries = ObjectOpenHashSet(queue)

        var index = 0
        while (queue.isNotEmpty() && index < entries.size) {
            val previous = entries[index]
            entries[index] = queue.removeLast()

            if (previous != null && !newEntries.contains(previous)) {
                queue.addFirst(previous)
            }

            index++
        }
    }

    companion object {
        const val NOT_FOUND = -1
        private const val DEFAULT_CAPACITY = 128

        fun createDefault() = MessageSignatureCache(DEFAULT_CAPACITY)
    }
}
