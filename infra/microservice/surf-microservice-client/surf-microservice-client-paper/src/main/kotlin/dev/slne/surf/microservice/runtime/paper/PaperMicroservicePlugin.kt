package dev.slne.surf.microservice.runtime.paper

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin

abstract class PaperMicroservicePlugin: SuspendingJavaPlugin() {
    override suspend fun onLoadAsync() {

    }

    override suspend fun onEnableAsync() {

    }

    override suspend fun onDisableAsync() {

    }
}