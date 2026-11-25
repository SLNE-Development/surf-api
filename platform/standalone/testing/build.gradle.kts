plugins {
    `core-convention`
}

description = "surf-api-standalone-testing"

dependencies {
    api(projects.surfApiStandaloneImpl)
    api(libs.spring.boot.test)
    api(libs.assertj.core)

    compileOnly(libs.junit.platform.launcher)
}
