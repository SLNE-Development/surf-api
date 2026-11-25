package dev.slne.surf.api.core.server.impl.luckperms

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.model.user.UserManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.BeansException
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

private const val NOT_RUNNING =
    "LuckPerms is not running yet, or this Minestom server does not call withLuckPerms()"

class SurfLuckPermsImplTest {

    private val uuid = UUID.randomUUID()
    private val userManager = mockk<UserManager>()
    private val luckPerms = mockk<LuckPerms>().also { every { it.userManager } returns userManager }
    private val user = mockk<User>()

    private fun surfLuckPerms(luckPerms: LuckPerms? = this.luckPerms): SurfLuckPermsImpl {
        val beanFactory = DefaultListableBeanFactory()

        if (luckPerms != null) {
            beanFactory.registerSingleton("luckPerms", luckPerms)
        }

        return SurfLuckPermsImpl(beanFactory.getBeanProvider(LuckPerms::class.java))
    }

    @Test
    fun `throws IllegalStateException without a LuckPerms bean`() {
        val failure = assertThrows<IllegalStateException> { surfLuckPerms(null).getUser(uuid) }

        assertEquals(NOT_RUNNING, failure.message)
    }

    @Test
    fun `throws IllegalStateException while the lazy LuckPerms bean cannot be created yet`() {
        val beanFactory = DefaultListableBeanFactory()
        val definition = RootBeanDefinition(LuckPerms::class.java) {
            error("The LuckPerms API isn't loaded yet!")
        }
        definition.isLazyInit = true
        beanFactory.registerBeanDefinition("luckPerms", definition)
        val surfLuckPerms = SurfLuckPermsImpl(beanFactory.getBeanProvider(LuckPerms::class.java))

        val failure = assertThrows<IllegalStateException> { surfLuckPerms.prefix(uuid) }

        assertEquals(NOT_RUNNING, failure.message)
        assertIs<BeansException>(failure.cause)
    }

    @Test
    fun `reads the loaded user's prefix, suffix and meta`() {
        every { userManager.getUser(uuid) } returns user
        every { user.cachedData.metaData.prefix } returns "<red>Admin "
        every { user.cachedData.metaData.suffix } returns null
        every { user.cachedData.metaData.getMetaValue("rank") } returns "3"

        val surfLuckPerms = surfLuckPerms()

        assertEquals("<red>Admin ", surfLuckPerms.prefix(uuid))
        assertEquals("", surfLuckPerms.suffix(uuid))
        assertEquals("3", surfLuckPerms.metaValue(uuid, "rank"))
    }

    @Test
    fun `falls back to empty values when the user is not loaded`() {
        every { userManager.getUser(uuid) } returns null

        val surfLuckPerms = surfLuckPerms()

        assertNull(surfLuckPerms.getUser(uuid))
        assertEquals("", surfLuckPerms.prefix(uuid))
        assertNull(surfLuckPerms.metaValue(uuid, "rank"))
    }

    @Test
    fun `loads a user that is not in memory`() = runTest {
        every { userManager.getUser(uuid) } returns null
        every { userManager.loadUser(uuid) } returns CompletableFuture.completedFuture(user)

        assertSame(user, surfLuckPerms().getOrLoadUser(uuid))
    }

    @Test
    fun `returns a user in memory without loading it`() = runTest {
        every { userManager.getUser(uuid) } returns user

        assertSame(user, surfLuckPerms().getOrLoadUser(uuid))
        verify(exactly = 0) { userManager.loadUser(any<UUID>()) }
    }
}
