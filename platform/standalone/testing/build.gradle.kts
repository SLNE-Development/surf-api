plugins {
    `core-convention`
}

description = "surf-api-standalone-testing"

dependencies {
    api(projects.surfApiStandaloneImpl)

    compileOnly(libs.junit.platform.launcher)
}
