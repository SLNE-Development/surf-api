# surf-api-standalone-testing

Starts the standalone runtime once per JUnit Platform launcher session.

- `SurfApiStandaloneTestSession`, a `LauncherSessionListener` registered as a service
- `surfContextRunner()`, a Spring Boot `ApplicationContextRunner` whose contexts resemble a plugin
  context below the running root, for testing conditions with and without classes and beans

Add it as a test dependency to run tests against surf-api without a server. See the
`surf-api-standalone` skill.
