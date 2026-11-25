package dev.slne.surf.api.paper.server.nms.v26_1.reflection

import dev.slne.surf.api.paper.server.nms.v26_1.packet.listener.V26_1ChannelInjector
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Binds every reflection proxy of this module against the server classes it compiles against.
 */
@Suppress("ClassName")
class V26_1NmsReflectionsTest {

    @Test
    fun `every reflected member resolves`() {
        assertNotNull(V26_1NmsReflections.toString())
        assertNotNull(V26_1ChannelInjector.ReadablePlayerCookieConnectionImplProxy.instance)
    }
}
