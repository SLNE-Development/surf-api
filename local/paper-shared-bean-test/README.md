# surf-api-paper-shared-bean-test

A boot test that downloads a Paper server and pinned plugin jars, installs the provider and consumer
plugins, and checks that a `@PluginApi` bean crosses from one plugin to the other.

Local only, because it downloads and boots a real server. A checksum mismatch makes the test skip.
The server assembly comes from `local/paper-server-harness`, and the `paper-server-test` convention
pins the downloads.

```shell
./gradlew :surf-api-paper-shared-bean-test:paperSharedBeanBootTest
```
