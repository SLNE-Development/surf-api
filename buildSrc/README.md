# buildSrc

The convention plugins and checks of the surf-api build.

- the roles `surf.module-api`, `surf.module-impl` and `surf.aggregate`, and `core-convention`
- `api-validation`, `surf.nms-shared-26` and the publishing, toolchain, test and vendor conventions
- `checkApiBoundary`, `checkInlineInternalApi`, `checkAbiDumpNesting` and `CheckShadowServiceFile`
- the shadow helpers for Spring registries, NATS relocation and `shadowed(...)`

The main build does not run the tests of `buildSrc`; run `./gradlew -p buildSrc test`. See the
`surf-api-build` skill.
