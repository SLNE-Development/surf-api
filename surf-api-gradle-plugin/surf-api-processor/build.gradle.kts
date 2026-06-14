// region properties
val groupId = findProperty("group") as String
val snapshot = (findProperty("snapshot") as String).toBooleanStrict()
// endregion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `publish-convention`
    `java-toolchain-convention`
}

group = groupId
version = buildString {
    append("1.0.1")
    if (snapshot) append("-SNAPSHOT")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.auto.service.annotations)
    api(projects.surfApiShared.surfApiSharedInternal)

    // https://mvnrepository.com/artifact/com.squareup/kotlinpoet
    implementation("com.squareup:kotlinpoet:2.3.0")
    implementation("com.palantir.javapoet:javapoet:0.7.0")
    implementation(libs.kotlin.compiler)
}