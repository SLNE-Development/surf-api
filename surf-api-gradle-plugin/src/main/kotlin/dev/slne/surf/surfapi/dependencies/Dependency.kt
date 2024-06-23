package dev.slne.surf.surfapi.dependencies

data class Dependency(
    val version: String,
    val group: String,
    val name: String,
    val relocationPattern: List<String> = listOf(),
    val relocationPackage: String = name
) {
    val notation: String
        get() = "$group:$name:$version"
}
