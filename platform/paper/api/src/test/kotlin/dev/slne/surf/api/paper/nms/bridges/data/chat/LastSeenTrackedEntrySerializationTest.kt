package dev.slne.surf.api.paper.nms.bridges.data.chat

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.data.chat.LastSeenMessagesValidatorMirror.LastSeenTrackedEntry
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(NmsUseWithCaution::class)
class LastSeenTrackedEntrySerializationTest {

    @Test
    fun `the pending flag keeps its wire name`() {
        val entry = LastSeenTrackedEntry(byteArrayOf(1), isPending = true)

        assertEquals("""{"signature":[1],"pending":true}""", Json.encodeToString(entry))
    }

    @Test
    fun `an entry written under the pending key decodes`() {
        val decoded = Json.decodeFromString<LastSeenTrackedEntry>(
            """{"signature":[1],"pending":true}""",
        )

        assertEquals(LastSeenTrackedEntry(byteArrayOf(1), isPending = true), decoded)
    }
}
