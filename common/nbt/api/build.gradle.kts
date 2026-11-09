// surf-api's NBT surface: the `CollectionBinaryTag` view over Adventure's list-shaped tags, the
// `FastCompoundBinaryTag` contract and the `InternalNbtBridge` that hands one out, the binary-tag
// extension functions, and `NbtOps` -- the DataFixerUpper `DynamicOps` implementation for Adventure
// binary tags.
//
// Nothing here names a contract still inside `surf-api-core-api`, so core re-exports the module
// with `api(...)` and everything compiling against surf-api-core keeps resolving the tag
// declarations unchanged.
//
// `NbtOps` is the file the serialization cut had to leave behind: it reads `nbt.asCollection` and
// `nbt.isCollectionTag`, so taking it to `surf-api-serialization-api` would have made that module
// depend on `surf-api-core-api`, which re-exports it -- a cycle. With the nbt domain out of core
// the file travels with the tags it reads instead, keeping its `serializer` directory.
//
// `core.reflection` was NOT taken along, although the plan paired the two. The code says they are
// two domains: `nbt` is Adventure binary tags and `reflection` is dynamic proxy generation, they
// share not one import, neither names a declaration of the other, and their consumers do not
// overlap -- nbt is read by core's own runtime, reflection by the four Paper NMS bridges, Velocity
// and the standalone bootstrap. Filing `SurfReflection` under `dev.slne.surf.api.nbt` would have
// named it after a domain it has nothing to do with, so it became `surf-api-reflection-api`.
//
// The implementations stay in `surf-api-core-impl`, next to the reflection ones, for the reason the
// messages and event cuts gave: nothing compiles against them, so a module of their own would buy
// no boundary.
//
// The libraries below are the ones the moved files name, each in the configuration
// `surf-api-core-api` declared it in, so a consumer's compile classpath is what it was before the
// move. The DataFixerUpper relocation is `surf-api-serialization-api`'s, because `NbtOps` is the
// one file of that cut that ended up here.
plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    // `InternalNbtBridge.INSTANCE` resolves the bridge with `surfBean`.
    api(projects.surfApiDiApi)
    // `InternalSurfApi`, which marks the bridge and which the `surfBean` call opts into.
    api(projects.surfApiAnnotations)

    // Every declaration here is about `net.kyori.adventure.nbt` types and names them in its own
    // signatures.
    api(libs.adventure.nbt)
    // `BinaryTagHolder` and `net.kyori.adventure.util.Codec`, which the extension functions take.
    compileOnlyApi(libs.adventure.api)

    // `NbtOps` is a `com.mojang.serialization.DynamicOps`.
    api(libs.datafixerupper) { isTransitive = false }
}

tasks {
    shadowJar {
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("com.mojang.serialization", "$relocationPrefix.mojang.serialization")
        relocate("com.mojang.datafixers", "$relocationPrefix.mojang.datafixers")
    }
}

description = "surf-api-nbt-api"
