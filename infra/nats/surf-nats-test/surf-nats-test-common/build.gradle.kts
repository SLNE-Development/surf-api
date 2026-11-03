import dev.slne.surf.api.gradle.util.slneReleases

plugins {
    id("dev.slne.surf.api.gradle.core")
    id("dev.slne.surf.nats")
}

// Published module: publish uploads the contracts to weaver first (after generation and the compatibility check
// against the previous weaver release), then the jar to Maven.
weaver {
    publishing {
        name = "surf-nats-test-common"
    }
}

publishing {
    repositories {
        slneReleases()
    }
}

// The publish manifest declares surf-nats-types-common at this version, so the registry must know it first.
tasks.publishProtos {
    mustRunAfter(":surf-nats-types:surf-nats-types-common:publishProtos")
}
