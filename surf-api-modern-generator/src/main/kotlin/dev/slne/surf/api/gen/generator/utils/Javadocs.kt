package dev.slne.surf.api.gen.generator.utils

object Javadocs {

    fun getVersionDependentClassHeader(headerIdentifier: String): String {
        return """
            Vanilla keys for $headerIdentifier.
            
            @apiNote The fields provided here are a direct representation of
            what is available from the vanilla game source. They may be
            changed (including removals) on any Minecraft version
            bump, so cross-version compatibility is not provided on the
            same level as it is on most of the other API.
        """.trimIndent()
    }

    fun getVersionDependentField(headerIdentifier: String): String {
        return """
            $headerIdentifier
            
            @apiNote This field is version-dependent and may be removed in future Minecraft versions
        """.trimIndent()
    }
}
