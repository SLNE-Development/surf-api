plugins {
    `java-library`
}

group = "me.lucko"
version = libs.versions.sparkMinestom.get()

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

dependencies {
    api(libs.minestom.spark.common)

    compileOnly(libs.minestom.server)
    compileOnly(libs.slf4j)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("pluginVersion", pluginVersion)

    filesMatching("me/lucko/spark/minestom/spark-minestom.properties") {
        expand("version" to pluginVersion)
    }
}
