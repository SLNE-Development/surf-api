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
val mangledPrefix: String = nettyRelocationBase
    .replace("_", "_1")
    .replace(".", "_")

subprojects {
    tasks.withType<ShadowJar>().configureEach {
        val base = "dev.slne.surf.redis.libs."

        relocate("io.netty", nettyRelocationBase + "io.netty")

        relocate("com.esotericsoftware", base + "kryo")
        relocate("io.reactivex", base + "reactivex")
        relocate("javax.cache", base + "javax.cache")
        relocate("jodd", base + "jodd")
        relocate("net.bytebuddy", base + "bytebuddy")
        relocate("org.objenesis", base + "objenesis")
        relocate("org.redisson", base + "redisson")
        relocate("org.yaml", base + "yaml")
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
    }
}