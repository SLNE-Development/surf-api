/**
 * Convention for a module that provides implementations.
 *
 * An implementation module never reaches a consumer's compile classpath: the aggregator pulls it in
 * at runtime and it is shaded into the final jar. The convention therefore adds nothing to the core
 * convention beyond making the role explicit, and asserts that the module's name actually marks it
 * as an implementation — otherwise [surfModuleRole] would classify it as an API module and
 * `checkApiBoundary` in the modules depending on it would pass for the wrong reason.
 */

plugins {
    id("core-convention")
}

require(surfModuleRole(project.name) == SurfModuleRole.IMPL) {
    "${project.path} applies surf.module-impl, but its name does not mark it as an implementation " +
            "module. Expected a name ending in -impl or -internal, or matching -nms-v<digit>."
}

/**
 * Checks the bean metadata of everything this module ships with.
 *
 * Wired here rather than per module because an implementation module is where beans come from, and
 * its runtime classpath is the closest the build gets to the set a container will actually merge.
 * [CheckBeanGraph] explains what it judges and what it leaves to the container.
 */
val checkBeanGraph = tasks.register<CheckBeanGraph>("checkBeanGraph") {
    group = "verification"
    description = "Checks the merged bean metadata of this module's runtime classpath."

    classpath.from(configurations.named("runtimeClasspath"))
    classpath.from(tasks.named("jar"))
    report.set(layout.buildDirectory.file("reports/bean-graph.txt"))
}

tasks.named("check") { dependsOn(checkBeanGraph) }
