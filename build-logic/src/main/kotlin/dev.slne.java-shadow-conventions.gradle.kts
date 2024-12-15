import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

tasks {
    withType<ShadowJar> {
        mergeServiceFiles()
    }
}
