# surf-api-nats-spring

The Spring integration. `NatsContextContributor` gives every plugin and microservice context a `NatsClient` and service
proxies, and registers `@NatsService` and `@NatsListener` beans. Bundled into each platform's surf-api plugin.

See [surf-nats](../README.md#modules) for the library and all of its modules.
