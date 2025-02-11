import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14" apply false
    alias(libs.plugins.dokka)
}

allprojects {
    repositories {
        mavenCentral()
    }

    if (subprojects.isEmpty()) {
        apply(plugin = rootProject.libs.plugins.dokka.get().pluginId)
    }
}

tasks {
    dokkaHtml {
        outputDirectory.set(layout.buildDirectory.dir("documentation/html"))
    }

    withType<DokkaTask>().configureEach {
        dokkaSourceSets.configureEach {
            documentedVisibilities = setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED
            )
        }
    }

    register<Copy>("publishDokkaToDocs") {
        dependsOn(dokkaHtmlMultimodule)
        from(buildDir.resolve("dokka/htmlMultiModule"))
        into(rootDir.resolve("docs"))
    }
}

