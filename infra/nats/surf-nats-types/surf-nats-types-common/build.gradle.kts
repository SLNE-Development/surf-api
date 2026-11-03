import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.core")
    alias(libs.plugins.weaver)
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

// Published to weaver so that contracts importing these protos validate on the registry. The conventions match those
// of the dev.slne.surf.nats plugin; the plugin jar bundles the same src/main/proto directory.
weaver {
    url = providers.environmentVariable("WEAVER_URL").orElse("https://weaver.slne.dev")
    repositories("proto-releases-public")
    publishing {
        repository = "proto-releases-public"
        group = "dev.slne.surf.nats"
        name = "surf-nats-types-common"
        source = layout.projectDirectory.dir("src/main/proto")
    }
}

publishing {
    repositories {
        slneReleases()
    }
}

tasks.publish {
    dependsOn(tasks.publishProtos)
}

tasks.withType<PublishToMavenRepository>().configureEach {
    mustRunAfter(tasks.publishProtos)
}
