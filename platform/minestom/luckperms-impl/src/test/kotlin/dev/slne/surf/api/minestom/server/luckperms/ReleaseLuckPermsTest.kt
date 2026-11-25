package dev.slne.surf.api.minestom.server.luckperms

import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.permission.PermissionProvider
import io.mockk.mockk
import net.kyori.adventure.util.TriState
import net.luckperms.api.LuckPerms
import org.springframework.beans.factory.getBeanProvider
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import kotlin.test.*

class ReleaseLuckPermsTest {

    private val root = AnnotationConfigApplicationContext().apply { refresh() }
    private val featureProvider = PermissionProvider { _, _ -> TriState.TRUE }

    @AfterTest
    fun reset() {
        MinestomPermissions.installProvider(null)
        root.close()
    }

    @Test
    fun `the feature's own provider is uninstalled and the bean removed`() {
        MinestomPermissions.installProvider(featureProvider)
        LuckPermsRootBean.register(root, mockk<LuckPerms>())

        releaseLuckPerms(featureProvider, root)

        assertNotSame(featureProvider, MinestomPermissions.provider)
        assertNull(root.getBeanProvider<LuckPerms>().ifAvailable)
    }

    @Test
    fun `a provider the server installed afterwards survives`() {
        val serverProvider = PermissionProvider { _, _ -> TriState.FALSE }
        MinestomPermissions.installProvider(serverProvider)
        LuckPermsRootBean.register(root, mockk<LuckPerms>())

        releaseLuckPerms(featureProvider, root)

        assertSame(serverProvider, MinestomPermissions.provider)
        assertNull(root.getBeanProvider<LuckPerms>().ifAvailable)
    }

    @Test
    fun `without a root context only the provider is released`() {
        MinestomPermissions.installProvider(featureProvider)

        releaseLuckPerms(featureProvider, root = null)

        assertNotSame(featureProvider, MinestomPermissions.provider)
    }
}
