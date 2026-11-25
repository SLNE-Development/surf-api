package dev.slne.surf.api.minestom.server.luckperms

import net.luckperms.api.LuckPerms
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.support.GenericApplicationContext

/**
 * Adds the running LuckPerms api to surf-api's refreshed root context and removes it again.
 *
 * Beans that resolve `LuckPerms` per call, such as through an `ObjectProvider` in a plugin
 * context, see the singleton from [register] until [remove].
 */
internal object LuckPermsRootBean {

    const val NAME = "luckPerms"

    /**
     * Registers [luckPerms] as the singleton [NAME] of [root].
     *
     * @throws IllegalStateException if [root] already holds a bean named [NAME]
     */
    fun register(root: ApplicationContext, luckPerms: LuckPerms) {
        beanFactory(root).registerSingleton(NAME, luckPerms)
    }

    /**
     * Removes the singleton [NAME] from [root]; does nothing when it is not registered.
     */
    fun remove(root: ApplicationContext) {
        beanFactory(root).destroySingleton(NAME)
    }

    private fun beanFactory(root: ApplicationContext): DefaultListableBeanFactory {
        return (root as GenericApplicationContext).defaultListableBeanFactory
    }
}
