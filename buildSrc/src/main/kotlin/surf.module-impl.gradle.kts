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
            "module. Expected a name ending in -impl (or, until the modules are renamed, " +
            "containing -server, ending in -internal, or matching -nms-v<digit>)."
}
