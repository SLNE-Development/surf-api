import java.util.zip.ZipFile

plugins {
    id("dev.slne.surf.surfapi.gradle.core")
}

val sanitizedLibsDir = layout.buildDirectory.dir("sanitized-libs")
val sanitizeLibs by tasks.registering {
    inputs.files(fileTree("libs") { include("*.jar") })
    outputs.dir(sanitizedLibsDir)

    doLast {
        val outDir = sanitizedLibsDir.get().asFile
        outDir.mkdirs()

        fileTree("libs").matching { include("*.jar") }.files.forEach { inJar ->
            val hasProvider = ZipFile(inJar).use { zf ->
                zf.getEntry("META-INF/services/org.slf4j.spi.SLF4JServiceProvider") != null ||
                        zf.getEntry("org/slf4j/simple/SimpleServiceProvider.class") != null
            }

            val dest = outDir.resolve(inJar.name)
            if (!hasProvider) {
                inJar.copyTo(dest, overwrite = true)
            } else {
                val tmp = layout.buildDirectory.dir("tmp/sanitize/${inJar.nameWithoutExtension}")
                    .get().asFile
                tmp.deleteRecursively(); tmp.mkdirs()

                copy {
                    from(zipTree(inJar))
                    exclude(
                        "META-INF/services/org.slf4j.spi.SLF4JServiceProvider",
                        "org/slf4j/simple/**"
                    )
                    into(tmp)
                }
                ant.invokeMethod(
                    "zip",
                    mapOf("basedir" to tmp.absolutePath, "destfile" to dest.absolutePath)
                )
            }
        }
    }
}

tasks.jar {
    dependsOn(sanitizeLibs)

    from({
        sanitizedLibsDir.get().asFileTree.files.map { zipTree(it) }
    })
}

dependencies {
    api(projects.surfMicroserviceApi.surfMicroserviceApiCommon)

    api(fileTree(sanitizedLibsDir) { include("*.jar") }.builtBy(sanitizeLibs))
}