package dev.slne.surf.api.standalone.testing

import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap
import kotlinx.coroutines.runBlocking
import org.junit.platform.launcher.LauncherSession
import org.junit.platform.launcher.LauncherSessionListener

/**
 * Starts surf-api's standalone runtime once per test JVM and stops it after the last test.
 *
 * Registered as a JUnit Platform service, so it applies to every test classpath that contains
 * `surf-api-standalone-testing`. Tests can then use surf-api's beans and the API that reads them,
 * such as config classes, which work only while the root context runs.
 */
class SurfApiStandaloneTestSession : LauncherSessionListener {
    override fun launcherSessionOpened(session: LauncherSession) {
        runBlocking {
            SurfApiStandaloneBootstrap.bootstrap()
            SurfApiStandaloneBootstrap.enable(registerShutdownHook = false)
        }
    }

    override fun launcherSessionClosed(session: LauncherSession) {
        runBlocking {
            SurfApiStandaloneBootstrap.shutdown()
        }
    }
}
