/**
 * Name of the configuration an aggregate uses to collect implementation modules.
 *
 * It feeds `runtimeOnly` and the shadow jar, so the implementations ship with the aggregate without
 * appearing on a consumer's compile classpath.
 */
const val SURF_RUNTIME = "surfRuntime"
