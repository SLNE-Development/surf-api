plugins {
    kotlin("jvm")
    `publish-convention`
}

val groupId = findProperty("group") as String
val snapshot = (findProperty("snapshot") as String).toBooleanStrict()

group = groupId
version = buildString {
    append("2.3.20-ij253-119")
    append("-1.0.0")
    if (snapshot) append("-SNAPSHOT")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:${libs.versions.kotlinVersion.get()}")
}

kotlin {
    jvmToolchain(21)
}