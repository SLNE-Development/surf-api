// region properties
val mcVersion: String by project
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
    append(mcVersion)
    append("-1.0.0")
    if (snapshot) append("-SNAPSHOT")
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.auto.service.annotations)
    api(project(":surf-api-shared:surf-api-shared-internal"))

    // https://mvnrepository.com/artifact/com.squareup/kotlinpoet
    implementation("com.squareup:kotlinpoet:2.3.0")
}