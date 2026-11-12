// Test support for code that runs on surf-api's standalone runtime: putting this module on a test
// runtime classpath starts that runtime once per test JVM, see SurfApiStandaloneTestSession.
plugins {
    `core-convention`
}

description = "surf-api-standalone-testing"

dependencies {
    api(projects.surfApiStandaloneImpl)

    // The launcher is on every JUnit Platform test runtime already.
    compileOnly(libs.junit.platform.launcher)
}
