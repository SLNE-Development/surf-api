plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiInternal)
    api(projects.surfApiCoreApi)
    api(projects.surfApiSpringImpl)
    api(projects.surfApiConfigImpl)
    compileOnly(libs.packetevents.netty.common)
    api(libs.bytebuddy)

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.kotlin.coroutines)
    implementation(libs.bundles.kotlin.coroutines.reactor)
    implementation(libs.bundles.kotlin.serialization)
    implementation(libs.adventure.nbt)
    implementation(libs.glm)
    implementation(libs.caffeine.courotines)

    testImplementation(libs.luckperms)
    testRuntimeOnly(libs.flogger)
    testRuntimeOnly(libs.flogger.slf4j.backend)
}

description = "surf-api-core-impl"
