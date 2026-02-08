package dev.slne.surf.surfapi.gradle.platform.standalone

import dev.slne.surf.surfapi.gradle.platform.SurfApiPlatform
import dev.slne.surf.surfapi.gradle.platform.core.AbstractCoreSurfPlugin

internal class StandaloneSurfPlugin :
    AbstractCoreSurfPlugin<StandaloneSurfExtension>("standalone", SurfApiPlatform.STANDALONE) {
    override val extensionClass = StandaloneSurfExtension::class.java
}