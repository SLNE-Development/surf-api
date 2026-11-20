plugins {
    id("surf.module-impl")
}

dependencies {
    api(projects.surfApiSpringApi)

    api(libs.kotlin.reflect)
    api(libs.kotlinxCoroutines.core)

    testImplementation(libs.spring.test)
}

description = "surf-api-spring-impl"
