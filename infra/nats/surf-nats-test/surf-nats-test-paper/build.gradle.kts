import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.util.withSurfApiBukkit

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
    id("dev.slne.surf.nats")
}

evaluationDependsOn(":surf-nats-platform:surf-nats-paper")

surfPaperPluginApi {
    mainClass("dev.slne.surf.nats.test.paper.PaperMain")
    authors.addAll("twisti")

    runServer {
        withSurfApiBukkit()
        pluginJars(project(":surf-nats-platform:surf-nats-paper").tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
    }
}

dependencies {
    implementation(projects.surfNatsTest.surfNatsTestCommon)
}
