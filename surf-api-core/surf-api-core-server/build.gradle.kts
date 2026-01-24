plugins {
    `core-convention`
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    implementation(project(":surf-api-common"))
    compileOnly(libs.packetevents.netty.common)
}


kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.surfapi.core.api.util.InternalSurfApi")
    }
}

description = "surf-api-core-server"
