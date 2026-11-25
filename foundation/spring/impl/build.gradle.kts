plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiSpringApi)

    implementation(platform(libs.spring.framework.bom))
    implementation(libs.spring.context)
    implementation(libs.kotlin.stdlib)

    api(libs.kotlin.reflect)
    api(libs.kotlinxCoroutines.core)

    testImplementation(libs.spring.test)
}

description = "surf-api-spring-impl"
