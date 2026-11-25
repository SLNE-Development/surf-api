package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import dev.slne.surf.api.paper.server.nms.v1_21_11.packet.listener.V1_21_11ChannelInjector
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Binds every reflection proxy of this module against the server classes it compiles against.
 */
@Suppress("ClassName")
class V1_21_11NmsReflectionsTest {

    @Test
    fun `every reflected member resolves`() {
        assertNotNull(V1_21_11NmsReflections.toString())
        assertNotNull(V1_21_11ChannelInjector.ReadablePlayerCookieConnectionImplProxy.instance)
    }
}
