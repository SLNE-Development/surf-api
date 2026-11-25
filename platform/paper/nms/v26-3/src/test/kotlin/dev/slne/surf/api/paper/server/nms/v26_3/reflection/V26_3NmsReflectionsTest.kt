package dev.slne.surf.api.paper.server.nms.v26_3.reflection

import dev.slne.surf.api.paper.server.nms.v26_3.packet.listener.V26_3ChannelInjector
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Binds every reflection proxy of this module against the server classes it compiles against.
 */
@Suppress("ClassName")
class V26_3NmsReflectionsTest {

    @Test
    fun `every reflected member resolves`() {
        assertNotNull(V26_3NmsReflections.toString())
        assertNotNull(V26_3ChannelInjector.ReadablePlayerCookieConnectionImplProxy.instance)
    }
}
