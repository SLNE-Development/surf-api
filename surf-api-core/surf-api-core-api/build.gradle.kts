plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.text.logger.slf4j)
}

//tasks {
//    val tokens = mapOf(
//        "versions.guava" to libs.versions.guava.version.getCore(),
//        "versions.caffeine" to libs.versions.caffeine.version.getCore(),
//        "versions.gson" to libs.versions.gson.version.getCore(),
//        "versions.commons-lang3" to libs.versions.commons.lang3.version.getCore(),
//        "versions.commons-text" to libs.versions.commons.text.version.getCore(),
//        "packet-events-spigot" to libs.versions.packetevents.version.getCore(),
//        "entity-lib" to libs.versions.entitylib.version.getCore(),
//        "scoreboard-library" to libs.versions.scoreboard.library.version.getCore(),
//        "project.version" to project.version.toString(),
//    )
//
//    val properties = Properties()
//    tokens.forEach { (key, value) -> properties["\${$key}"] = value }
//
//    withType<ProcessResources> {
//        // all files in src/main/resources will be processed not only paper-plugin.yml
//        this.
//        filesMatching("**/*") {
//            throw StopExecutionException(this.toString())
//            println("#########################")
//            println("file: $this")
//            filter {
//                var placeHolder = it
//                tokens.forEach { (key, value) -> placeHolder = placeHolder.replace(key, value) }
//                placeHolder
//            }
//            println("#########################")
//        }
//    }
//}

description = "surf-api-core-api"
