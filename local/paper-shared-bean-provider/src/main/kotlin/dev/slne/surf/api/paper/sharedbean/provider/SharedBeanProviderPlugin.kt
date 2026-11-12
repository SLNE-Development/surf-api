package dev.slne.surf.api.paper.sharedbean.provider

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.di.SurfContainer

/**
 * The providing plugin of the shared-bean boot test.
 *
 * It does nothing but run its own container through the three phases the platform has, which is
 * all a plugin has to do to publish a [Scope.SHARED][dev.slne.surf.api.di.Scope.SHARED] bean:
 * [SurfContainer.startChild] reads this jar's own bean metadata and creates
 * [SharedGreetingImpl], and because that bean is shared it lands in surf-api's root container
 * where every other plugin's container can resolve it.
 */
class SharedBeanProviderPlugin : SuspendingJavaPlugin() {

    override suspend fun onLoadAsync() {
        SurfContainer.startChild(this)
    }

    override suspend fun onEnableAsync() {
        SurfContainer.enableFor(this)
    }

    override suspend fun onDisableAsync() {
        SurfContainer.stopFor(this)
    }
}
