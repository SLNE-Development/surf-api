# GitHub Copilot Instructions for SURF-API

## Project Overview

SURF-API is a multi-platform Minecraft server API framework providing unified abstractions across different server platforms (Bukkit/Paper, Hytale, Velocity). It enables cross-server communication and provides standardized interfaces for common server operations.

## Technology Stack

- **Language**: Kotlin 2.3.0 (JVM, Java 25 target)
- **Build System**: Gradle 8.x with Kotlin DSL
- **Key Frameworks**: Paper/Bukkit API, Velocity API, Hytale API
- **Async/Reactive**: Kotlin Coroutines, Reactor-Netty
- **Networking**: OkHttp, Ktor Client
- **Text/UI**: Adventure (rich text components)
- **Code Generation**: KSP (Kotlin Symbol Processing), AutoService

## Module Structure

The project follows an API/Implementation separation pattern:

```
surf-api-core/          # Core abstractions
├── surf-api-core-api
└── surf-api-core-server

surf-api-bukkit/        # Bukkit/Paper implementation
├── surf-api-bukkit-api
├── surf-api-bukkit-server
└── surf-api-bukkit-plugin-test

surf-api-velocity/      # Velocity proxy implementation
surf-api-hytale/        # Hytale implementation (future)
surf-api-standalone/    # Standalone server implementation
surf-api-gradle-plugin/ # Build tooling and annotation processing
buildSrc/              # Gradle convention plugins
```

## Coding Conventions

### Package Naming
- Base package: `dev.slne.surf.surfapi`
- Platform-specific: `dev.slne.surf.surfapi.{platform}.api` and `.server`
- Shaded dependencies: `dev.slne.surf.surfapi.libs.*`

### Kotlin Style
- Use Kotlin idioms: data classes, sealed classes, extension functions
- Prefer immutability and null-safety
- Use coroutines for async operations
- Follow Kotlin naming conventions (camelCase for functions, PascalCase for classes)

### API Design
- **Public API modules** (-api): Contain interfaces and public contracts
- **Implementation modules** (-server): Contain sealed implementations, not exported
- Mark internal APIs with `@InternalSurfApi` annotation
- Use extensive KDoc comments for public APIs
- Follow interface-based design with companion object singletons

### Service Loading
Use `@AutoService` for platform-specific implementations:
```kotlin
@AutoService(SurfCoreApi::class)
class SurfStandaloneApiImpl : SurfCoreApiImpl()
```

## Build Commands

```bash
# Build all modules
./gradlew build shadowJar

# Run tests
./gradlew test

# Check API compatibility
./gradlew check checkLegacyAbi

# Publish to local Maven
./gradlew publishToMavenLocal

# Run Paper test server
./gradlew :surf-api-bukkit:surf-api-bukkit-plugin-test:runServer
```

## Versioning

- Version format: `{MC_VERSION}-{LIBRARY_VERSION}` (e.g., `1.21.11-2.55.2`)
- Automatic ABI compatibility checks via `checkLegacyAbi`
- API validation enabled to prevent breaking changes
- Version defined in `gradle.properties`

## Testing Guidelines

- Integration tests exist in `surf-api-bukkit-plugin-test`
- Use Paper's `runServer` task for manual testing
- Validate changes against multiple platform implementations
- Ensure API compatibility with existing versions

## Dependencies

- Shaded dependencies are relocated to `dev.slne.surf.surfapi.libs.*`
- Platform APIs are provided (not shaded): Paper, Velocity, Hytale
- Check `gradle/libs.versions.toml` for version catalogs
- Always check for existing utilities before adding new dependencies

## CI/CD

- **PR validation**: Build, API checks, ABI dumps
- **Publishing**: Tagged releases publish to SLNE Maven registry
- **Java version**: CI uses Java 25 GraalVM
- Workflows located in `.github/workflows/`

## Important Notes

1. **Maintain API compatibility**: Use `@Deprecated` and versioning for breaking changes
2. **Platform separation**: Keep platform-specific code in respective modules
3. **Shadow plugin**: All dependencies except platform APIs are shaded
4. **KSP processing**: Code generation happens during build via KSP
5. **Adventure components**: Use Adventure API for all text formatting and components
6. **Async operations**: Use Kotlin coroutines, not callbacks or futures
7. **Configuration**: Use Configurate for YAML/config handling

## Code Generation

The project uses KSP for code generation:
- Service provider files (META-INF/services)
- Annotation processing for custom annotations
- Generated code is in `build/generated/ksp/`

## When Making Changes

1. **Check module boundaries**: API changes belong in -api modules
2. **Update version**: Increment library version for API changes
3. **Run ABI checks**: Ensure `checkLegacyAbi` passes
4. **Test across platforms**: Validate on Bukkit, Velocity when applicable
5. **Document**: Add KDoc for new public APIs
6. **Follow patterns**: Match existing code structure and conventions
