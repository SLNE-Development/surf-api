import org.gradle.api.artifacts.ProjectDependency

/**
 * Convention for a module that declares contracts consumers compile against.
 *
 * Adds ABI validation on top of the core convention and registers [CheckApiBoundary], which keeps
 * the module from depending on an implementation module. The check runs as part of `check` and
 * fails the build; `-Psurf.apiBoundary.strict=false` downgrades it to a warning.
 */

plugins {
    id("api-validation")
}

/** Dependency declarations that reach a consumer's compile or runtime classpath. */
val checkedConfigurations = listOf(
    "api",
    "implementation",
    "compileOnly",
    "compileOnlyApi",
    "runtimeOnly",
)

val checkApiBoundary = tasks.register<CheckApiBoundary>("checkApiBoundary") {
    group = "verification"
    description = "Fails when this API module depends on an implementation module."

    modulePath.set(project.path)
    strict.set(
        providers.gradleProperty("surf.apiBoundary.strict")
            .map { it.toBooleanStrict() }
            .orElse(true)
    )
    violations.set(emptySet<String>())
}

// The dependencies block of the consuming build file runs after this plugin is applied, so the
// declarations can only be read once the project is fully configured.
afterEvaluate {
    val found = checkedConfigurations
        .mapNotNull { configurations.findByName(it) }
        .flatMap { configuration ->
            configuration.dependencies
                .filterIsInstance<ProjectDependency>()
                .filter { surfModuleRole(it.name) == SurfModuleRole.IMPL }
                .map { "${configuration.name} -> ${it.name}" }
        }
        .toSortedSet()

    checkApiBoundary.configure { violations.set(found) }
}

tasks.named("check") {
    dependsOn(checkApiBoundary)
}
