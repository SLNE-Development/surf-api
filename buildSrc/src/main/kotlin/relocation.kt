import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

/**
 * Publishes a relocation marker for each of [oldArtifactIds] alongside this module's own artifact.
 *
 * A consumer that still resolves one of the old coordinates gets a POM with no artifact and a
 * `<distributionManagement><relocation>` block pointing at this module, so the build keeps
 * resolving and the warning names the new coordinate. Use it for a module whose published name
 * changed and that consumers resolve directly; a module only ever pulled in transitively needs no
 * marker.
 */
fun Project.publishRelocationFrom(vararg oldArtifactIds: String) {
    val publishing = extensions.getByType<PublishingExtension>()

    oldArtifactIds.forEach { oldArtifactId ->
        require(oldArtifactId != name) {
            "$path declares a relocation from its own artifact id '$oldArtifactId'."
        }

        val publicationName = "relocation" + oldArtifactId
            .split('-')
            .joinToString("") { part -> part.replaceFirstChar { it.uppercase() } }

        publishing.publications.create<MavenPublication>(publicationName) {
            artifactId = oldArtifactId

            pom {
                name.set(oldArtifactId)
                description.set("Relocated to ${project.group}:${project.name}.")

                distributionManagement {
                    relocation {
                        groupId.set(project.group.toString())
                        artifactId.set(project.name)
                        version.set(project.version.toString())
                        message.set("The module was renamed to ${project.name}.")
                    }
                }
            }
        }
    }
}
