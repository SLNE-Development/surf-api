plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("dev.slne.java-common-conventions")

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

dependencies {
    compileOnlyApi("org.jetbrains:annotations:24.1.0")
    compileOnlyApi("com.google.guava:guava:32.1.2-jre")
    compileOnlyApi("com.github.ben-manes.caffeine:caffeine:3.1.8")
    compileOnlyApi("com.google.code.gson:gson:2.10.1")
    compileOnlyApi("org.apache.commons:commons-lang3:3.13.0")
    compileOnlyApi("org.apache.commons:commons-text:1.11.0")
    compileOnlyApi("it.unimi.dsi:fastutil:8.5.12")
}