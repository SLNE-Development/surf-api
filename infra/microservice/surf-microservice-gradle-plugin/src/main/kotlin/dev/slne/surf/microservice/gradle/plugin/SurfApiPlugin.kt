package dev.slne.surf.microservice.gradle.plugin

enum class SurfApiPlugin(
    pluginName: String
) {
    CORE("core"),
    PAPER_RAW("paper-raw"),
    PAPER_PLUGIN("paper-plugin"),
    VELOCITY("velocity"),
    STANDALONE("standalone"),
    SETTINGS("settings");

    val pluginName: String = "dev.slne.surf.surfapi.gradle.$pluginName"
}