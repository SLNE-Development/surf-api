import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.core")
    id("dev.slne.surf.api.gradle.nats")
}

weaver {
    publishing {
        name = "surf-api-nats-test-common"
    }
}

publishing {
    repositories {
        slneReleases()
    }
}

tasks.publishProtos {
    mustRunAfter(":surf-api-nats:surf-api-nats-types:surf-api-nats-types-common:publishProtos")
}
