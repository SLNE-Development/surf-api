# surf-api-bom

A Gradle platform that aligns every published surf-api module, the infra modules included, to one
version.

Modules built only outside CI are left out; see `notPublished` in `build.gradle.kts`.
