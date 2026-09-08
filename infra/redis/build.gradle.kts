import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

buildscript {
    repositories {
        gradlePluginPortal()
        maven("https://reposilite.slne.dev/public/") { name = "public" }
    }
    dependencies {
        classpath("dev.slne.surf.api:surf-api-gradle-plugin:+")
    }
}

allprojects {
    group = "dev.slne.surf.redis"
    version = findProperty("version") as String
}

val nettyRelocationBase = "dev.slne.surf.redis.shaded."
val libsRelocationBase = "dev.slne.surf.redis.libs."
val mangledPrefix: String = nettyRelocationBase
    .replace("_", "_1")
    .replace(".", "_")

val shadedPackages = mapOf(
    "io.netty" to nettyRelocationBase + "io.netty",
    "com.esotericsoftware" to libsRelocationBase + "kryo",
    "io.reactivex" to libsRelocationBase + "reactivex",
    "javax.cache" to libsRelocationBase + "javax.cache",
    "jodd" to libsRelocationBase + "jodd",
    "net.bytebuddy" to libsRelocationBase + "bytebuddy",
    "org.objenesis" to libsRelocationBase + "objenesis",
    "org.redisson" to libsRelocationBase + "redisson",
    "org.yaml" to libsRelocationBase + "yaml",
)

extra["shadedPackages"] = shadedPackages

subprojects {
    tasks.withType<ShadowJar>().configureEach {
        shadedPackages.forEach { (from, to) -> relocate(from, to) }
    }

    tasks.withType<ShadowJar>().configureEach {
        doLast {
            val jar = archiveFile.get().asFile
            if (!jar.exists()) return@doLast

            val tmpJar = File(jar.parentFile, "${jar.name}.tmp")

            ZipFile(jar).use { zipIn ->
                ZipOutputStream(tmpJar.outputStream()).use { zipOut ->
                    for (entry in zipIn.entries()) {
                        val name = entry.name

                        val newName = if (
                            name.startsWith("META-INF/native/") &&
                            name != "META-INF/native/" &&
                            name.contains("netty_")
                        ) {
                            val fileName = name.substringAfter("META-INF/native/")
                            val nettyIndex = fileName.indexOf("netty_")
                            if (nettyIndex >= 0) {
                                val before = fileName.substring(0, nettyIndex)
                                val after = fileName.substring(nettyIndex)
                                "META-INF/native/$before$mangledPrefix$after"
                            } else {
                                name
                            }
                        } else {
                            name
                        }

                        val newEntry = ZipEntry(newName)
                        newEntry.time = entry.time
                        if (entry.method == ZipEntry.STORED) {
                            newEntry.method = ZipEntry.STORED
                            newEntry.size = entry.size
                            newEntry.crc = entry.crc
                            newEntry.compressedSize = entry.compressedSize
                        }
                        zipOut.putNextEntry(newEntry)
                        zipIn.getInputStream(entry).use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                    }
                }
            }

            jar.delete()
            tmpJar.renameTo(jar)
        }
    }

    afterEvaluate {
        configure<ShadowExtension> {
            addShadowVariantIntoJavaComponent = false
        }

        configure<KotlinJvmExtension> {
            compilerOptions {
                optIn.add("dev.slne.surf.redis.util.InternalRedisAPI")
            }
        }

        if (extensions.findByType<PublishingExtension>()?.publications?.findByName("shadow") != null) {
            tasks.withType<AbstractPublishToMaven>().configureEach {
                onlyIf("only the shadow publication owns these coordinates") {
                    publication.name != "pluginMaven"
                }
            }
        }
    }
}