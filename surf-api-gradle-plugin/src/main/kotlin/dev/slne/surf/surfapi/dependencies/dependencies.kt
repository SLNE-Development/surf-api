package dev.slne.surf.surfapi.dependencies

// ---------------------------------------- Versions ---------------------------------------- //
// <editor-fold desc="Versions">
private const val adventureVersion = "4.17.0"
private const val dazzleconfVersion = "1.3.0-M2"
private const val spongepoweredMathVersion = "2.0.1"
private const val okhttpVersion = "4.12.0"
private const val configurateVersion = "4.1.2"
private const val packetEventsVersion = "2.3.0"
private const val fastutilVersion = "8.5.13"
private const val commandApiVersion = "9.5.1"
private const val commandApiSnapshotVersion = "9.5.0-SNAPSHOT"
private const val brigadierVersion = "1.0.18"
private const val kotlinCoroutinesVersion = "1.9.0-RC"
private const val scoreboardLibraryVersion = "2.1.10"
private const val reflectionRemapperVersion = "0.1.1"
private const val morePersistentDataTypesVersion = "2.4.0"
private const val inventoryFrameworkVersion = "0.10.14"
private const val commonsLang3Version = "3.13.0"
private const val commonsTextVersion = "1.11.0"
private const val guavaVersion = "32.1.2-jre"
private const val caffeineVersion = "3.1.8"
private const val gsonVersion = "2.11.0"
// </editor-fold>

// ---------------------------------------- Surf Api ---------------------------------------- //
// <editor-fold desc="Surf Api">
val CORE_STANDALONE = VersionResolvableDependency(
    "dev.slne.surf",
    "surf-api-core-api-standalone",
    listOf("dev.slne.surf.surfapi.core.api"),
    "surfapi.core"
)
val CORE_API = VersionResolvableDependency(
    "dev.slne.surf",
    "surf-api-core-api"
)
val BUKKIT_API = VersionResolvableDependency(
    "dev.slne.surf",
    "surf-api-bukkit-api"
)
val VELOCITY_API = VersionResolvableDependency(
    "dev.slne.surf",
    "surf-api-velocity-api"
)
// </editor-fold>

// ---------------------------------------- Dependencies ---------------------------------------- //
// <editor-fold desc="Dependencies">
val ADVENTURE_API = adventure("adventure-api")
val ADVENTURE_TEXT_LOGGER_SLF4J = adventure("adventure-text-logger-slf4j")
val ADVENTURE_TEXT_MINIMESSAGE = adventure("adventure-text-minimessage")
val ADVENTURE_TEXT_SERIALIZER_GSON = adventure("adventure-text-serializer-gson")
val ADVENTURE_TEXT_SERIALIZER_LEGACY = adventure("adventure-text-serializer-legacy")
val ADVENTURE_TEXT_SERIALIZER_PLAIN = adventure("adventure-text-serializer-plain")
val ADVENTURE_TEXT_SERIALIZER_ANSI = adventure("adventure-text-serializer-ansi")

val DAZZLECONF = Dependency(
    dazzleconfVersion,
    "space.arim.dazzleconf",
    "dazzleconf-ext-snakeyaml",
    relocationPattern = listOf("space.arim.dazzleconf", "org.yaml.snakeyaml"),
    relocationPackage = "dazzleconf"
)
val SPONGEPOWERED_MATH = Dependency(
    spongepoweredMathVersion,
    "org.spongepowered",
    "math",
    relocationPattern = listOf("org.spongepowered.math")
)
val OKHTTP = Dependency(
    okhttpVersion,
    "com.squareup.okhttp3",
    "okhttp",
    relocationPattern = listOf("okhttp3")
)
val FASTUTIL = Dependency(
    fastutilVersion,
    "it.unimi.dsi",
    "fastutil",
    relocationPattern = listOf("it.unimi.dsi.fastutil")
)
val BRIGADIER = Dependency(
    brigadierVersion,
    "com.mojang",
    "brigadier",
)
val KOTLIN_COROUTINES = Dependency(
    kotlinCoroutinesVersion,
    "org.jetbrains.kotlinx",
    "kotlinx-coroutines-core"
)
val REFLECTION_REMAPPER = Dependency(
    reflectionRemapperVersion,
    "xyz.jpenilla",
    "reflection-remapper"
)
val MORE_PERSISTENT_DATA_TYPES = Dependency(
    morePersistentDataTypesVersion,
    "com.jeff_media",
    "MorePersistentDataTypes"
)
val INVENTORY_FRAMEWORK = Dependency(
    inventoryFrameworkVersion,
    "com.github.stefvanschie.inventoryframework",
    "IF"
)
val GUAVA = Dependency(
    guavaVersion,
    "com.google.guava",
    "guava",
    relocationPattern = listOf("com.google.common", "com.google.thirdparty.publicsuffix"),
    relocationPackage = "guava"
)
val CAFFEINE = Dependency(
    caffeineVersion,
    "com.github.ben-manes.caffeine",
    "caffeine",
    relocationPattern = listOf("com.github.benmanes.caffeine.cache"),
    relocationPackage = "caffeine"
)
val GSON = Dependency(
    gsonVersion,
    "com.google.code.gson",
    "gson",
    relocationPattern = listOf("com.google.gson"),
    relocationPackage = "gson"
)
val CONFIGURATE_YAML = configurate("yaml")
val CONFIGURATE_JACKSON = configurate("jackson")
val PACKET_EVENTS_API = packetEvents("api", "com.github.retrooper.packetevents")
val PACKET_EVENTS_SPIGOT = packetEvents("spigot", "io.github.retrooper.packetevents")
val PACKET_EVENTS_VELOCITY = packetEvents("velocity", "io.github.retrooper.packetevents")
val COMMAND_API_CORE = commandApi("core")
val COMMAND_API_BUKKIT = commandApi("bukkit-plugin")
val COMMAND_API_BUKKIT_KOTLIN = commandApi("bukkit-kotlin")
val COMMAND_API_VELOCITY = commandApi("velocity-core", commandApiSnapshotVersion)
val SCOREBOARD_LIBRARY_API = scoreboardLibrary("api")
val SCOREBOARD_LIBRARY_IMPLEMENTATION = scoreboardLibrary("implementation")
val SCOREBOARD_LIBRARY_MODERN = scoreboardLibrary("modern")
val COMMONS_LANG3 = apacheCommons(commonsLang3Version, "lang3")
val COMMONS_TEXT = apacheCommons(commonsTextVersion, "text")
// </editor-fold>

// ---------------------------------------- Functions ---------------------------------------- //
// <editor-fold desc="Functions">
private fun adventure(id: String) = Dependency(adventureVersion, "net.kyori", id)
private fun configurate(id: String) = Dependency(
    configurateVersion,
    "org.spongepowered",
    "configurate-$id",
    relocationPattern = listOf("org.spongepowered.configurate"),
    relocationPackage = "configurate"
)

private fun packetEvents(id: String, relocationPattern: String) = Dependency(
    packetEventsVersion,
    "com.github.retrooper.packetevents",
    id,
    relocationPattern = listOf(relocationPattern),
    relocationPackage = "packetevents.$id"
)
private fun commandApi(id: String, version: String = commandApiVersion) = Dependency(
    version,
    "dev.jorel",
    "commandapi-$id",
)
private fun scoreboardLibrary(id: String) = Dependency(
    scoreboardLibraryVersion,
    "net.megavex",
    "scoreboard-library-$id"
)
private fun apacheCommons(version: String, id: String) = Dependency(
    version,
    "org.apache.commons",
    "commons-$id",
    relocationPattern = listOf("org.apache.commons.$id"),
    relocationPackage = "commons.$id"
)
// </editor-fold>
