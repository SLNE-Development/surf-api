// This is an API module, but it cannot apply `surf.module-api` yet: it depends on
// surf-api-internal, which is an implementation module, and `checkApiBoundary` would rightly fail.
// surf-api-internal holds two unrelated things — the NMS provider metadata this module needs, and
// the old component framework's metadata schema — so it can only be dissolved once that framework
// is gone. Until then this module gets the plain convention and no boundary check.
plugins {
    `core-convention`
}

dependencies {
    compileOnlyApi(projects.surfApiPaperApi)
    compileOnlyApi(projects.surfApiInternal)
    compileOnly(libs.paper.api)
}
