import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Bundling
import org.gradle.kotlin.dsl.named

/**
 * Requests the shaded jar of [dependency] instead of its plain classes.
 *
 * For modules that publish only their shaded jar: code compiled against their published coordinate
 * sees the packages that jar relocates, and inside this build it has to see the same ones.
 */
fun Project.shadowed(dependency: ProjectDependency): ModuleDependency =
    dependency.copy().attributes {
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED))
    }
