/**
 * Convention for a module that runs the surf KSP processor.
 *
 * The processor generates the implementations behind `@GenerateReflection` interfaces and the NMS
 * provider index under `META-INF/surf-api/nms-providers` for `@NmsProviderMarker` classes. Every
 * module that declares either annotation must apply this convention. Without it the module still
 * compiles, but `generatedReflectionAccessor` and `NmsProvider` find nothing at runtime.
 */

plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    ksp(project(":surf-api-processor"))
}
