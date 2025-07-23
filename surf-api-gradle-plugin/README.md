# Installation
Add the following to your root `build.gradle.kts` file:

```kotlin
buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://repo.slne.dev/repository/maven-public/") { name = "maven-public" }
    }
    dependencies {
        classpath("dev.slne.surf:surf-api-gradle-plugin:1.21.7+")
    }
}
```

And the following to your `gradle.properties` file:
````properties
kotlin.code.style=official
kotlin.stdlib.default.dependency=false
org.gradle.parallel=true
````

Apply the appropriate plugin in your module's `build.gradle.kts` file:

Bukkit example:
```kotlin
plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
}

surfPaperPluginApi { // name changes depending on the plugin type
    mainClass("dev.slne.surf.cloud.bukkit.BukkitMain") // required
    bootstrapper("dev.slne.surf.cloud.bukkit.BukkitBootstrap")
    authors.add("twisti")

    serverDependencies {
        registerRequired("LuckPerms")
    }

    runServer {
        jvmArgs("-Dsurf.cloud.serverName=test-server01")
    }
}
```

### Different plugin types

- `dev.slne.surf.surfapi.gradle.core` for core modules (applies kotlin and other plugins, adds surf-core-api compileOnly dependency)
- `dev.slne.surf.surfapi.gradle.paper-plugin` for paper plugin modules (all core features, adds paper-api dependency, generation of paper-plugin.yml and runServer configuration)
- `dev.slne.surf.surfapi.gradle.paper-raw` for paper plugin modules (all core features, adds paper-api dependency, **NO** generation of paper-plugin.yml nor runServer configuration)
- `dev.slne.surf.surfapi.gradle.standalone` for standalone modules (all core features, adds surf-core-api dependency, shades surf-api-standalone)
- `dev.slne.surf.surfapi.gradle.velocity` for velocity modules (all core features, adds velocity-api dependency)

### Easy copy
#### Core
```kotlin
plugins {
    id("dev.slne.surf.surfapi.gradle.core")
}
```

#### Paper Plugin
```kotlin
plugins {
    id("dev.slne.surf.surfapi.gradle.paper-plugin")
}
```

#### Paper Raw
```kotlin
plugins {
    id("dev.slne.surf.surfapi.gradle.paper-raw")
}
```

#### Standalone
```kotlin
plugins {
    id("dev.slne.surf.surfapi.gradle.standalone")
}
```

#### Velocity
```kotlin
plugins {
    id("dev.slne.surf.surfapi.gradle.velocity")
}
```
