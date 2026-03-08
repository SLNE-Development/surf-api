import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    id("dev.slne.surf.surfapi.gradle.standalone")
}

repositories {
    maven("https://repo.slne.dev/repository/maven-public/") {
        name = "maven-public"
    }
}

dependencies {
    api(projects.surfMicroserviceApi)
    implementation("org.apache.maven:maven-impl:4.0.0-rc-2")
    implementation("org.apache.maven.resolver:maven-resolver-supplier-mvn4:2.0.5")
}

tasks.named("shadowJar", ShadowJar::class.java) {
    mergeServiceFiles()

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}