package dev.slne.surf.api.core.server.impl.luckperms

import dev.slne.surf.api.luckperms.SurfLuckPerms
import dev.slne.surf.api.luckperms.getMetaValue
import dev.slne.surf.api.luckperms.prefix
import dev.slne.surf.api.luckperms.suffix
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.future.await
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import org.springframework.beans.BeansException
import org.springframework.beans.factory.ObjectProvider
import java.util.*

/**
 * Implements [SurfLuckPerms] on top of the `LuckPerms` bean of surf-api's root context.
 *
 * The class is no component, because platforms without LuckPerms on the class path must not load
 * it. Each platform that runs LuckPerms publishes it with a `@Bean` method.
 *
 * @param luckPermsProvider resolves the `LuckPerms` bean on every call, which a platform registers
 * lazily or only once LuckPerms has enabled
 */
@InternalSurfApi
class SurfLuckPermsImpl(
    private val luckPermsProvider: ObjectProvider<LuckPerms>,
) : SurfLuckPerms {

    override val luckPerms: LuckPerms
        get() {
            val luckPerms = try {
                luckPermsProvider.ifAvailable
            } catch (failure: BeansException) {
                throw IllegalStateException(NOT_RUNNING, failure)
            }

            return checkNotNull(luckPerms) { NOT_RUNNING }
        }

    override fun getUser(uuid: UUID): User? = luckPerms.userManager.getUser(uuid)

    override suspend fun loadUser(uuid: UUID): User = luckPerms.userManager.loadUser(uuid).await()

    override suspend fun getOrLoadUser(uuid: UUID): User = getUser(uuid) ?: loadUser(uuid)

    override fun prefix(uuid: UUID): String = getUser(uuid)?.prefix ?: ""

    override fun suffix(uuid: UUID): String = getUser(uuid)?.suffix ?: ""

    override fun metaValue(uuid: UUID, key: String): String? = getUser(uuid)?.getMetaValue(key)

    private companion object {
        const val NOT_RUNNING =
            "LuckPerms is not running yet, or this Minestom server does not call withLuckPerms()"
    }
}
