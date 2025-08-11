plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven("https://repo.slne.dev/repository/maven-releases/") {
            name = "maven-releases"
            credentials {
                username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
            }
        }
    }

    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}