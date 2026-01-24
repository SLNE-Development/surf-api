// region properties
val mcVersion: String by project
val groupId = findProperty("group") as String
val snapshot = (findProperty("snapshot") as String).toBooleanStrict()
// endregion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `publish-convention`
}

group = groupId
version = buildString {
    append(mcVersion)
    append("-1.0.0")
    if (snapshot) append("-SNAPSHOT")
}

dependencies {
    implementation(libs.kotlin.serialization.json)
}

description = "surf-api-common"
