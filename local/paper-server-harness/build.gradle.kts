plugins {
    `core-convention`
}

description = "surf-api-paper-server-harness"

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.junit.jupiter)

    implementation(libs.testcontainers.junit)
}
