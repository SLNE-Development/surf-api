plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
    id("io.papermc.paperweight.core") version "1.7.1"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

paperweight {
    paramMappingsRepo = paperMavenPublicUrl
    remapRepo = paperMavenPublicUrl
    decompileRepo = paperMavenPublicUrl
    minecraftVersion = "1.20.4"
}

dependencies {
    paramMappings("net.fabricmc:yarn:1.20.6+build.1:mergedv2")
    remapper("net.fabricmc:tiny-remapper:0.10.2:fat")
    decompiler("org.vineflower:vineflower:1.10.1")
//    spigotDecompiler("io.papermc:patched-spigot-fernflower:0.1+build.13")
    paperclip("io.papermc:paperclip:3.0.3")
}





