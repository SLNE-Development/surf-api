import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.paper-raw")
    alias(libs.plugins.weaver)
}

surfRawPaperApi {
    testing {
        mockBukkit.set(true)
        // The surf-api default (4.116.1) is not published for mockbukkit-v26.3.
        mockBukkitVersion.set("4.118.0")
    }
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    api(projects.surfNatsTypes.surfNatsTypesCommon)
    weaver(projects.surfNatsTypes.surfNatsTypesCommon)
}

// Published to weaver so that contracts importing these protos validate on the registry. The conventions match those
// of the dev.slne.surf.nats plugin; the plugin jar bundles the same src/main/proto directory.
weaver {
    url = providers.environmentVariable("WEAVER_URL").orElse("https://weaver.slne.dev")
    repositories("proto-releases-public")
    publishing {
        repository = "proto-releases-public"
        group = "dev.slne.surf.nats"
        name = "surf-nats-types-paper"
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

// The registry rejects a manifest whose dependency it does not know yet.
tasks.publishProtos {
    mustRunAfter(":surf-nats-types:surf-nats-types-common:publishProtos")
}

tasks.withType<PublishToMavenRepository>().configureEach {
    mustRunAfter(tasks.publishProtos)
}
