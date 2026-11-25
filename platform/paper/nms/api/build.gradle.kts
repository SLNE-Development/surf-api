plugins {
    `core-convention`
}

dependencies {
    compileOnlyApi(projects.surfApiPaperApi)
    compileOnlyApi(projects.surfApiInternal)
    compileOnly(libs.paper.api)
    compileOnly(platform(libs.netty.bom))
    compileOnly(libs.netty.transport)
    compileOnly(libs.netty.common)

    testImplementation(platform(libs.spring.framework.bom))
    testImplementation(libs.spring.context)
}
