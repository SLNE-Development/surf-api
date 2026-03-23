plugins {
    kotlin("jvm")
    `publish-convention`
    `java-toolchain-convention`
}

val groupId = findProperty("group") as String
val snapshot = (findProperty("snapshot") as String).toBooleanStrict()

group = groupId
version = buildString {
    append(libs.versions.kotlinVersion.get())
    append("-1.0.0")
    if (snapshot) append("-SNAPSHOT")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlinVersion.get()}")
}