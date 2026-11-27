package dev.slne.surf.api.paper.server.impl.inventory.temporary

import java.io.*
import java.util.*

/**
 * The byte form of a player's [StoredEntry], kept as one byte array in the player's persistent
 * data container.
 *
 * Every entry starts with its format version. [decode] reads only [VERSION] and reports any other
 * version as [DecodedEntry.UnknownVersion], so an entry written by a later surf-api is never
 * rewritten by this one. Item slots are opaque byte arrays; an empty array is an empty slot.
 */
internal object TemporaryInventoryFormat {

    const val VERSION = 1

    /**
     * @throws IOException if [entry] cannot be written
     */
    fun encode(entry: StoredEntry): ByteArray {
        val buffer = ByteArrayOutputStream()

        DataOutputStream(buffer).use { output ->
            output.writeInt(VERSION)
            output.writeInt(entry.sessions.size)

            for (session in entry.sessions) {
                output.writeSession(session)
            }

            output.writeInt(entry.retainedSlots.size)

            for (retained in entry.retainedSlots) {
                output.writeUuid(retained.sessionId)
                output.writeInt(retained.slot)
                output.writeBytes(retained.bytes)
            }
        }

        return buffer.toByteArray()
    }

    /**
     * @throws IOException if [bytes] are truncated or malformed
     */
    fun decode(bytes: ByteArray): DecodedEntry {
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            val version = input.readInt()
            if (version != VERSION) return DecodedEntry.UnknownVersion(version)

            val sessions = List(input.readCount()) { input.readSession() }
            val retainedSlots = List(input.readCount()) {
                RetainedSlot(
                    sessionId = input.readUuid(),
                    slot = input.readInt(),
                    bytes = input.readBytes(),
                )
            }

            return DecodedEntry.Supported(StoredEntry(sessions, retainedSlots))
        }
    }

    private fun DataOutputStream.writeSession(session: StoredSession) {
        writeUuid(session.id)
        writeUTF(session.owner)
        writeUTF(session.key)
        writeInt(session.selectedSlot)
        writeInt(session.slots.size)

        for (slot in session.slots) {
            writeBytes(slot)
        }

        writeBytes(session.state)
    }

    private fun DataInputStream.readSession(): StoredSession {
        val id = readUuid()
        val owner = readUTF()
        val key = readUTF()
        val selectedSlot = readInt()
        val slots = List(readCount()) { readBytes() }

        return StoredSession(id, owner, key, selectedSlot, slots, state = readBytes())
    }

    private fun DataOutputStream.writeUuid(uuid: UUID) {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }

    private fun DataInputStream.readUuid(): UUID = UUID(readLong(), readLong())

    private fun DataOutputStream.writeBytes(bytes: ByteArray) {
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readBytes(): ByteArray {
        val bytes = ByteArray(readCount())
        readFully(bytes)

        return bytes
    }

    private fun DataInputStream.readCount(): Int {
        val count = readInt()
        if (count < 0) throw IOException("Negative length $count in a temporary inventory entry")

        return count
    }
}

/**
 * The result of [TemporaryInventoryFormat.decode].
 */
internal sealed interface DecodedEntry {

    class Supported(val entry: StoredEntry) : DecodedEntry

    /**
     * An entry in a format version this surf-api does not know; it must stay untouched.
     */
    class UnknownVersion(val version: Int) : DecodedEntry
}

/**
 * Everything surf-api keeps about temporary inventory sessions in one player's data.
 *
 * Format version 1 allows at most one session; the list leaves room for stacked sessions.
 *
 * @property retainedSlots items of ended sessions that could not be put back, kept so that they are
 * never deleted
 */
internal class StoredEntry(
    val sessions: List<StoredSession>,
    val retainedSlots: List<RetainedSlot>,
) {

    val isEmpty: Boolean
        get() = sessions.isEmpty() && retainedSlots.isEmpty()

    fun session(id: UUID): StoredSession? = sessions.find { it.id == id }

    fun withSession(session: StoredSession): StoredEntry {
        return StoredEntry(sessions + session, retainedSlots)
    }

    /**
     * Removes the session [id] and keeps its [unreadable] slots.
     */
    fun withoutSession(id: UUID, unreadable: List<UnreadableSlot>): StoredEntry {
        val retained = unreadable.map { slot -> RetainedSlot(id, slot.slot, slot.bytes) }

        return StoredEntry(sessions.filterNot { it.id == id }, retainedSlots + retained)
    }
}

/**
 * The original inventory of one temporary inventory session.
 *
 * @property owner the name of the plugin that began the session
 * @property key the session's kind, as a namespaced key string
 * @property selectedSlot the selected hotbar slot, 0 to 8
 * @property slots the serialized item of every player inventory slot, in Bukkit's slot order; empty
 * for an empty slot
 * @property state the owning plugin's persistent data container, serialized
 */
internal class StoredSession(
    val id: UUID,
    val owner: String,
    val key: String,
    val selectedSlot: Int,
    val slots: List<ByteArray>,
    val state: ByteArray,
) {

    fun withState(state: ByteArray): StoredSession {
        return StoredSession(id, owner, key, selectedSlot, slots, state)
    }

    /**
     * Reads the stored slots for an inventory of [slotCount] slots.
     *
     * A slot that [read] rejects, and a non-empty slot beyond [slotCount], ends up in
     * [RestorePlan.unreadable] and stays empty in [RestorePlan.items]. Slots beyond the stored ones
     * stay empty.
     */
    fun <T : Any> planRestore(slotCount: Int, read: (ByteArray) -> T): RestorePlan<T> {
        val unreadable = mutableListOf<UnreadableSlot>()
        val items = List(slotCount) { slot ->
            val bytes = slots.getOrNull(slot)
            if (bytes == null || bytes.isEmpty()) return@List null

            try {
                read(bytes)
            } catch (failure: Exception) {
                unreadable += UnreadableSlot(slot, bytes, failure)
                null
            }
        }

        for (slot in slotCount until slots.size) {
            val bytes = slots[slot]
            if (bytes.isEmpty()) continue

            unreadable += UnreadableSlot(
                slot,
                bytes,
                IllegalStateException("The player inventory has no slot $slot"),
            )
        }

        return RestorePlan(items, unreadable)
    }
}

/**
 * An item of an ended session that could not be put back, kept in the player's data.
 */
internal class RetainedSlot(val sessionId: UUID, val slot: Int, val bytes: ByteArray)

/**
 * The outcome of [StoredSession.planRestore].
 *
 * @property items the item of every slot of the current inventory, `null` for an empty slot
 */
internal class RestorePlan<T : Any>(val items: List<T?>, val unreadable: List<UnreadableSlot>)

internal class UnreadableSlot(val slot: Int, val bytes: ByteArray, val cause: Exception)
