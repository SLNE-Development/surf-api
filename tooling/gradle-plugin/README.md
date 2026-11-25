# surf-api-gradle-plugin

The Gradle plugins that downstream projects apply. It is an included build, so the infra modules of
this repository apply the plugins the way a downstream project does.

| Plugin ID | Applied to |
| --- | --- |
| `dev.slne.surf.api.gradle.settings` | `settings.gradle.kts` |
| `dev.slne.surf.api.gradle.core` | a module that compiles against `surf-api-core` |
| `dev.slne.surf.api.gradle.paper-plugin`, `dev.slne.surf.api.gradle.paper-raw` | Paper plugins |
| `dev.slne.surf.api.gradle.velocity` | Velocity plugins |
| `dev.slne.surf.api.gradle.minestom` | Minestom plugins |
| `dev.slne.surf.api.gradle.minestom-server` | runnable Minestom servers |
| `dev.slne.surf.api.gradle.minestom-relocations` | a shadow jar that merges several Minestom plugins into one executable jar |
| `dev.slne.surf.api.gradle.standalone` | plain JVM services |
| `dev.slne.surf.api.gradle.nats` | NATS contracts: code generation, checks and the weaver registry |
| `dev.slne.surf.api.gradle.microservice` | microservices: the jar, the main class and the Dockerfile |

See the platform skills, `infra/nats/README.md` and `infra/microservice/README.md`.
