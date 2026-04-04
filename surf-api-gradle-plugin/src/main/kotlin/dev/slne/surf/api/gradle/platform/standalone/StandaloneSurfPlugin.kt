package dev.slne.surf.api.gradle.platform.standalone

import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import dev.slne.surf.api.gradle.platform.core.AbstractCoreSurfPlugin

internal class StandaloneSurfPlugin :
    AbstractCoreSurfPlugin<StandaloneSurfExtension>("standalone", SurfApiPlatform.STANDALONE) {
    override val extensionClass = StandaloneSurfExtension::class.java
}