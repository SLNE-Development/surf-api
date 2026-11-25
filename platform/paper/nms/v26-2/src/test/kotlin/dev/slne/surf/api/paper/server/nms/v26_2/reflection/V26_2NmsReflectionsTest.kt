package dev.slne.surf.api.paper.server.nms.v26_2.reflection

import dev.slne.surf.api.paper.server.nms.v26_2.packet.listener.V26_2ChannelInjector
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Binds every reflection proxy of this module against the server classes it compiles against.
 */
@Suppress("ClassName")
class V26_2NmsReflectionsTest {

    @Test
    fun `every reflected member resolves`() {
        assertNotNull(V26_2NmsReflections.toString())
        assertNotNull(V26_2ChannelInjector.ReadablePlayerCookieConnectionImplProxy.instance)
    }
}
