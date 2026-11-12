// The container's contracts: the annotations a bean carries, the lifecycle it may opt into, the
// lookup interface, and the metadata schema the KSP processor writes and the container reads. No
// implementation lives here, so a module can compile against the container without pulling it in.
//
// ABI validation stays off while the contracts are still being shaped; it goes on once they are
// consumed from outside this repository.
plugins {
    id("surf.module-api")
}

dependencies {
    // InternalSurfApi marks the metadata schema, which is a contract between the processor and the
    // container rather than something a consumer writes against.
    api(projects.surfApiAnnotations)
    api(libs.bundles.kotlin.serialization)
}

description = "surf-api-di-api"
