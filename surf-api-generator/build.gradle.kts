import jdk.tools.jlink.resources.plugins

plugins {
    java
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
    id("io.papermc.paperweight.userdev")
}

//plugins.apply(PaperweightSourceGeneratorHelper::class)

//extensions.configure(PaperweightSourceGeneratorExt::class) {
//    atFile.set(projectDir.toPath().resolve("wideners.at").toFile())
//}

dependencies {
    implementation("com.squareup:javapoet:1.13.0")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation(libs.paper.api)

    paperweight.paperDevBundle(libs.paper.api.get().version)
    implementation(paperweight.paperDevBundleDependency(libs.paper.api.get().version))

    implementation(project(":surf-api-core:surf-api-core-api"))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

//plugins.apply(PaperweightSourceGeneratorHelper::class)
//extensions.configure(PaperweightSourceGeneratorExt::class) {
//    atFile.set(projectDir.toPath().resolve("wideners.at").toFile())
//}

tasks.register<JavaExec>("generate") {
    dependsOn(tasks.check)
    mainClass.set("dev.slne.surf.api.gen.Main")
    classpath(sourceSets.main.map { it.runtimeClasspath })
    args(projectDir.toPath().resolve("generated").toString())
}




