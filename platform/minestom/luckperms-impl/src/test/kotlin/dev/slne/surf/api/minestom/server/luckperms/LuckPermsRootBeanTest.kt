package dev.slne.surf.api.minestom.server.luckperms

import io.mockk.mockk
import net.luckperms.api.LuckPerms
import org.springframework.beans.factory.getBeanProvider
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class LuckPermsRootBeanTest {

    private val root = AnnotationConfigApplicationContext().apply { refresh() }
    private val plugin = AnnotationConfigApplicationContext().apply {
        parent = root
        refresh()
    }

    @AfterTest
    fun close() {
        plugin.close()
        root.close()
    }

    @Test
    fun `a plugin context resolves the bean registered after the refresh until it is removed`() {
        val luckPerms = mockk<LuckPerms>()
        val provider = plugin.getBeanProvider<LuckPerms>()

        LuckPermsRootBean.register(root, luckPerms)
        assertSame(luckPerms, provider.ifAvailable)

        LuckPermsRootBean.remove(root)
        assertNull(provider.ifAvailable)
    }

    @Test
    fun `removing an unregistered bean does nothing`() {
        LuckPermsRootBean.remove(root)

        assertNull(plugin.getBeanProvider<LuckPerms>().ifAvailable)
    }
}
