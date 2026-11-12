package dev.slne.surf.api.paper.sharedbean.consumer

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.api.di.SurfContainer

/**
 * The consuming plugin of the shared-bean boot test.
 *
 * Its container is started in the load phase and enabled in the enable phase, like any plugin's.
 * The injection happens during [onLoadAsync]: a container is eager, so [SharedGreetingConsumer] is
 * constructed there, and a shared bean that is missing makes this call throw rather than leaving a
 * half-built container behind. [SharedGreetingConsumer.enable] then writes the line the test reads.
 */
class SharedBeanConsumerPlugin : SuspendingJavaPlugin() {

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
