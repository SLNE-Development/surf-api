# surf-api-standalone-testing

Starts the standalone runtime once per JUnit Platform launcher session.

- `SurfApiStandaloneTestSession`, a `LauncherSessionListener` registered as a service

Add it as a test dependency to run tests against surf-api without a server. See the
`surf-api-standalone` skill.
