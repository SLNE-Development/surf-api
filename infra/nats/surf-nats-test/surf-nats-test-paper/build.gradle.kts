import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dev.slne.surf.api.gradle.util.registerRequired
import dev.slne.surf.api.gradle.util.withSurfApiBukkit

plugins {
    id("dev.slne.surf.api.gradle.paper-plugin")
}

evaluationDependsOn(":surf-nats-paper")

surfPaperPluginApi {
    mainClass("dev.slne.surf.nats.test.paper.PaperMain")
    authors.addAll("twisti")

    serverDependencies {
        registerRequired("surf-nats-paper")
    }

    runServer {
        withSurfApiBukkit()
        pluginJars(project(":surf-nats-paper").tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
    }
}

dependencies {
    compileOnly(projects.surfNatsApi)
    implementation(projects.surfNatsTest.surfNatsTestCommon)
}
