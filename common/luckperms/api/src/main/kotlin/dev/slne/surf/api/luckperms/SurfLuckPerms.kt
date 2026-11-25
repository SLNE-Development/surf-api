package dev.slne.surf.api.luckperms

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfApiBean
import dev.slne.surf.api.spring.surfBean
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import java.util.*

/**
 * Reads LuckPerms users and their meta data.
 *
 * A bean of surf-api's root context on Paper and Velocity; inject it. Every member resolves the
 * running LuckPerms api on call, so the bean can be injected before LuckPerms has enabled.
 *
 * On Minestom the bean exists when the server build includes the LuckPerms feature module (Gradle
 * `withLuckPerms()`), and its members throw until the runtime feature (`withLuckPerms {}` in
 * `surfMinestomServer`) has loaded. Without the module there is no bean; inject
 * `ObjectProvider<SurfLuckPerms>` there.
 */
@SurfApiBean
interface SurfLuckPerms {

    /**
     * The running LuckPerms api.
     *
     * @throws IllegalStateException if LuckPerms is not running
     */
    val luckPerms: LuckPerms

    /**
     * Returns the user with [uuid] if LuckPerms holds them in memory, which it does for every
     * online player.
     *
     * @param uuid the player's UUID
     * @return the loaded user, or `null` if LuckPerms has not loaded them
     * @throws IllegalStateException if LuckPerms is not running
     */
    fun getUser(uuid: UUID): User?

    /**
     * Loads the user with [uuid] from LuckPerms' storage, also when they are already loaded.
     *
     * @param uuid the player's UUID
     * @return the loaded user; a player LuckPerms has never seen gets a user with default data
     * @throws IllegalStateException if LuckPerms is not running
     */
    suspend fun loadUser(uuid: UUID): User

    /**
     * Returns the user with [uuid] from memory, or loads them from storage when they are not
     * loaded, such as an offline player.
     *
     * @param uuid the player's UUID
     * @return the user
     * @throws IllegalStateException if LuckPerms is not running
     */
    suspend fun getOrLoadUser(uuid: UUID): User

    /**
     * Returns the prefix of the loaded user with [uuid].
     *
     * @param uuid the player's UUID
     * @return the prefix, or an empty string if the user has none or is not loaded
     * @throws IllegalStateException if LuckPerms is not running
     */
    fun prefix(uuid: UUID): String

    /**
     * Returns the suffix of the loaded user with [uuid].
     *
     * @param uuid the player's UUID
     * @return the suffix, or an empty string if the user has none or is not loaded
     * @throws IllegalStateException if LuckPerms is not running
     */
    fun suffix(uuid: UUID): String

    /**
     * Returns the meta value under [key] of the loaded user with [uuid].
     *
     * @param uuid the player's UUID
     * @param key the meta key
     * @return the value, or `null` if the user has no such meta or is not loaded
     * @throws IllegalStateException if LuckPerms is not running
     */
    fun metaValue(uuid: UUID, key: String): String?

    companion object {
        /**
         * The [SurfLuckPerms] bean of surf-api's installed root context.
         *
         * @throws IllegalStateException if no root context is installed and no bean was read before
         */
        @InternalSurfApi
        val instance by surfBean<SurfLuckPerms>()
    }
}
