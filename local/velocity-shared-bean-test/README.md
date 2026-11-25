# surf-api-velocity-shared-bean-test

A boot test that downloads a Velocity proxy and pinned plugin jars, installs the provider and
consumer plugins, and checks that a `@PluginApi` bean crosses from one plugin to the other.

Local only, because it downloads and boots a real server. A checksum mismatch makes the test skip.
