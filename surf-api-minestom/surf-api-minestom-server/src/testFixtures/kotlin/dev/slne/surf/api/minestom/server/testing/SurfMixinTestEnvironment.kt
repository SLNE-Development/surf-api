package dev.slne.surf.api.minestom.server.testing

import dev.slne.surf.api.minestom.server.impl.mixin.SurfMixinAgent
import net.bytebuddy.agent.ByteBuddyAgent
import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * Installs Mixin into the test JVM when the JUnit launcher session opens, like the server jar's
 * bootstrap does, so tests run against the transformed Minestom classes.
 *
 * Registered as a [LauncherSessionListener], so depending on the test fixtures of
 * `surf-api-minestom-server` is enough. Applies the surf mixin configurations found on the test
 * class path plus those listed, comma separated, in the [CONFIGS_PROPERTY] system property.
 */
class SurfMixinTestEnvironment : LauncherSessionListener {
    override fun launcherSessionOpened(session: LauncherSession) {
        install()
    }

    companion object {
        /**
         * System property listing further mixin configurations to apply, comma separated.
         */
        const val CONFIGS_PROPERTY = "surf.test.mixin-configs"

        private val SURF_CONFIGS = listOf(
            "mixins.surf-api-minestom-server.json",
            "mixins.surf-api-minestom-server-npc.json",
        )

        /**
         * The mixin configurations applied in this JVM.
         */
        val configurations: List<String> by lazy {
            val loader = SurfMixinTestEnvironment::class.java.classLoader
            val extra = System.getProperty(CONFIGS_PROPERTY).orEmpty()
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)
            SURF_CONFIGS.filter { loader.getResource(it) != null } + extra
        }

        /**
         * Installs Mixin unless it is already installed.
         */
        @JvmStatic
        @Synchronized
        fun install() {
            if (SurfMixinAgent.isInstalled()) return
            SurfMixinAgent.install(ByteBuddyAgent.install(), configurations)
        }
    }
}
