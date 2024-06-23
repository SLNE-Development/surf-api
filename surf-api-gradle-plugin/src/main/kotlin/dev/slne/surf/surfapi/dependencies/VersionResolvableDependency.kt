package dev.slne.surf.surfapi.dependencies

data class VersionResolvableDependency(
    val group: String,
    val name: String,
    val relocationPattern: List<String> = listOf(),
    val relocationPackage: String = name
) {
    fun resolve(version: String) =
        Dependency(version, group, name, relocationPattern, relocationPackage)
}