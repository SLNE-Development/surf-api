# SURF-API Development Instructions

SURF-API is a comprehensive Minecraft plugin API framework supporting Paper/Bukkit and Velocity servers. This is a multi-module Gradle project written in Kotlin and Java that provides extensive APIs for Minecraft plugin development.

**ALWAYS follow these instructions first and fallback to additional search and context gathering only if the information here is incomplete or found to be in error.**

## Project Structure Overview

This is a multi-module Gradle project with the following key modules:
- `surf-api-core` (core-api, core-server) - Core framework APIs and implementation
- `surf-api-bukkit` (bukkit-api, bukkit-server, bukkit-plugin-test) - Paper/Bukkit server support
- `surf-api-velocity` (velocity-api, velocity-server) - Velocity proxy server support  
- `surf-api-standalone` - Standalone server implementation
- `surf-api-gradle-plugin` - Gradle plugin for SURF development
- `surf-api-generator` & `surf-api-modern-generator` - Code generation tools

## System Requirements

**CRITICAL**: This project requires Java 21. Check your Java version with `java --version`.
- If using Java 17 or lower, some modules will fail to build
- The project uses Gradle 9.0.0 which requires minimum Java 17
- Target compilation is Java 21 as configured in gradle.properties

## Initial Setup and Dependencies

Always run these commands first when working with a fresh clone:

```bash
# Verify Java version (MUST be 21 for full compatibility)
java --version

# Verify Gradle wrapper works
./gradlew --version

# Check project structure
./gradlew projects
```

## Build Process

### Known Build Limitations

**CRITICAL BUILD ISSUE**: The project currently cannot build completely due to missing external dependencies:
- Paper MC development bundle version `1.21.8-R0.1-SNAPSHOT` is not available
- External repositories (repo.slne.dev, repo.papermc.io) may not be accessible
- Network connectivity is required for builds

### Build Commands

**NEVER CANCEL builds or long-running commands. Gradle builds may take 5-15 minutes on first run.**

**WARNING**: All full build commands will currently fail due to missing Paper MC 1.21.8-R0.1-SNAPSHOT dependency and network access issues.

```bash
# Full clean build (WILL FAIL due to dependency issues)
# NEVER CANCEL: Set timeout to 20+ minutes
./gradlew clean build --parallel --no-scan

# Try building without problematic Bukkit modules (WILL STILL FAIL)
# NEVER CANCEL: Set timeout to 15+ minutes  
./gradlew clean build -x :surf-api-bukkit:surf-api-bukkit-server:build --parallel --no-scan

# Generate shadow JARs for distribution (WILL FAIL)
# NEVER CANCEL: Set timeout to 20+ minutes
./gradlew shadowJar --parallel --no-scan

# Check if Gradle configuration works (usually fails at configuration phase)
./gradlew projects

# Verify Gradle wrapper and build system
./gradlew --version
```

### Working Commands (Limited Functionality)

**CRITICAL**: Almost all Gradle commands fail due to configuration issues during the project evaluation phase.

The following commands work reliably:

```bash
# Verify Gradle wrapper and version (works)
./gradlew --version

# Check Java version (works)
java --version

# Explore project structure without Gradle
find . -name "*.kt" -o -name "*.java" | head -20
find . -name "build.gradle.kts" -exec echo "=== {} ===" \; -exec head -5 {} \;
ls -la surf-api-core/surf-api-core-api/src/main/kotlin/dev/slne/surf/surfapi/core/api/

# View configuration files
cat gradle/libs.versions.toml
cat gradle.properties
cat settings.gradle.kts
```

**Commands that FAIL** (due to project configuration issues):
- `./gradlew help` - Fails during configuration phase
- `./gradlew properties` - Fails during configuration phase  
- `./gradlew tasks` - Fails during configuration phase
- `./gradlew projects` - Fails during configuration phase
- Any build-related command

### Repository Dependencies

The project depends on these external repositories:
- `repo.slne.dev/repository/maven-public/` - SLNE Development private repository
- `repo.papermc.io/repository/maven-public/` - Paper MC repository
- Maven Central

If builds fail with repository access errors, this is expected in isolated environments.

## Testing

**LIMITATION**: No traditional unit test files were found in the repository.
- The project includes a `surf-api-bukkit-plugin-test` module for runtime testing
- Testing primarily occurs through integration testing with Minecraft servers
- **ALL test commands will fail due to the same dependency resolution issues affecting builds**

```bash
# Run any available tests (WILL FAIL due to dependency issues)
# NEVER CANCEL: Set timeout to 15+ minutes
./gradlew test --parallel --no-scan

# Run checks (code quality, compatibility validation) (WILL FAIL)
# NEVER CANCEL: Set timeout to 10+ minutes
./gradlew check --parallel --no-scan
```

## Development Workflow

### Working with the Codebase

**Primary Source Locations**:
- Core APIs: `surf-api-core/surf-api-core-api/src/main/kotlin/dev/slne/surf/surfapi/core/api/`
- Bukkit APIs: `surf-api-bukkit/surf-api-bukkit-api/src/main/kotlin/dev/slne/surf/surfapi/bukkit/api/`
- Velocity APIs: `surf-api-velocity/surf-api-velocity-api/src/main/kotlin/dev/slne/surf/surfapi/velocity/api/`

**Build Configuration**:
- Version catalog: `gradle/libs.versions.toml`
- Build conventions: `buildSrc/src/main/kotlin/`
- Module builds: `*/build.gradle.kts` files

### Code Quality and CI

```bash
# Run Kotlin API binary compatibility validation (WILL FAIL)
# NEVER CANCEL: Set timeout to 10+ minutes
./gradlew apiCheck

# Generate API dump for compatibility checking (WILL FAIL)
./gradlew apiDump
```

### Key Dependencies

- **Kotlin**: 2.2.0 with coroutines support
- **Paper API**: 1.21.8-R0.1-SNAPSHOT (currently unavailable)
- **Velocity API**: 3.4.0-SNAPSHOT
- **Adventure API**: 4.24.0 for text components
- **PacketEvents**: 2.9.1 for packet manipulation
- **Command API**: 10.1.2 for command handling

## Common Development Tasks

### Adding New Modules

1. Add module declaration to `settings.gradle.kts`
2. Create `build.gradle.kts` using appropriate convention plugin:
   - `core-convention` for basic modules
   - Check `buildSrc/src/main/kotlin/` for available conventions

### Working with the Gradle Plugin

The project includes its own Gradle plugin at `surf-api-gradle-plugin/`. See `surf-api-gradle-plugin/README.md` for usage instructions.

### Validation After Changes

**ALWAYS run these validation steps before committing (NOTE: Most will fail due to dependency issues)**:

```bash
# Verify no compilation errors (WILL FAIL due to dependency resolution)
./gradlew compileKotlin compileJava

# Check API compatibility (WILL FAIL)
./gradlew apiCheck

# Verify Gradle plugin builds (WILL FAIL)
./gradlew :surf-api-gradle-plugin:build

# Alternative: Only verify wrapper and configuration work
./gradlew --version
./gradlew help
```

## Manual Validation Scenarios

**Due to fundamental build system limitations, validation is extremely restricted:**

**What CAN be validated**:
1. **Source Code Syntax**: Manually review Kotlin/Java files for syntax errors
2. **File Organization**: Check that new files are in appropriate module directories
3. **Import Statements**: Verify import statements reference existing classes
4. **Version Catalog Consistency**: Check `gradle/libs.versions.toml` for version conflicts
5. **Configuration Changes**: Review build script modifications for syntax

**Validation Commands That Work**:
```bash
# Check file structure and organization
find . -name "*.kt" -path "*/api/*" | head -10
find . -name "*.kt" -path "*/server/*" | head -10

# Review recent changes
git status
git diff

# Validate configuration file syntax
# (Manually review build.gradle.kts files for Kotlin syntax)
```

**What CANNOT be validated** (due to build failures):
- Code compilation
- Test execution  
- API compatibility checking
- JAR generation
- Dependency resolution
- Plugin functionality

## GitHub Actions CI/CD

The repository includes these workflows:
- `publish.yml` - Publishes artifacts and creates GitHub releases
- `build-pr-jar.yml` - Builds JARs for pull requests with `build-pr-jar` label
- `api-dump-version.yml` - Validates API compatibility

**CI builds may fail in forks due to missing repository credentials and dependency access.**

## Troubleshooting

### Build Failures

**Root Cause**: The project depends on Paper MC development bundle version `1.21.8-R0.1-SNAPSHOT` which is either:
1. Not yet released/available
2. Requires network access to private repositories
3. The version specification in `gradle/libs.versions.toml` is ahead of available releases

**Typical Error Messages**:
- `Could not resolve io.papermc.paper:dev-bundle:1.21.8-R0.1-SNAPSHOT`
- `repo.slne.dev: No address associated with hostname`
- `repo.papermc.io: No address associated with hostname`

**Most common issues**:
1. **Java version mismatch**: Ensure Java 21 is installed and active  
2. **Network dependencies**: External repositories are inaccessible in sandboxed environments
3. **Paper MC version**: Version 1.21.8-R0.1-SNAPSHOT does not exist or is not publicly available

### Working Around Limitations

**For Code Development**:
1. **Use IDE analysis**: IntelliJ IDEA or VS Code can provide syntax checking and imports without building
2. **Focus on source code review**: Examine Kotlin/Java files directly for logic and API changes
3. **Manual validation**: Check imports, syntax, and API usage patterns manually
4. **Version catalog validation**: Review `gradle/libs.versions.toml` for dependency consistency

**For Understanding Project Structure**:
```bash
# Explore module structure
find . -name "build.gradle.kts" -exec echo "=== {} ===" \; -exec head -10 {} \;

# Check source code organization  
find . -type d -name "kotlin" | grep src/main

# Review API definitions
find . -path "*/api/*" -name "*.kt" | head -10
```

### Working Around Limitations

When full builds fail:
1. Focus on code compilation validation: `./gradlew compileKotlin compileJava`
2. Work with source code directly for logic changes
3. Test changes by examining compiled output when possible
4. Use IDE analysis for immediate feedback on syntax and imports

### Version Updates

To update dependency versions:
1. Edit `gradle/libs.versions.toml`
2. Update corresponding Paper MC version in gradle.properties `mcVersion` property
3. Ensure Paper development bundle exists for the target version

## Important Notes

- **NEVER CANCEL** any Gradle command - builds may take 15+ minutes
- Set explicit timeouts of 60+ minutes for all build operations
- **CRITICAL LIMITATION**: The project cannot be built in standard sandboxed environments due to:
  - Missing Paper MC development bundle (1.21.8-R0.1-SNAPSHOT)
  - Required access to private repositories (repo.slne.dev)
  - Network connectivity requirements
- **ALL Gradle build commands will fail** during project configuration phase
- **Focus on source code analysis** when build system is unavailable
- The repository is actively developed - dependency versions may be ahead of public releases
- **Only basic file system operations and Git commands work reliably**
- Consider this a **source-only repository** for analysis purposes in restricted environments

## Alternative Development Approaches

When the build system is not functional:

1. **IDE Analysis**: Use IntelliJ IDEA or VS Code with Kotlin support for syntax checking
2. **Source Code Review**: Focus on code structure, API design, and implementation logic
3. **Documentation**: Work with README files, configuration files, and comments
4. **Version Management**: Update dependency versions in `gradle/libs.versions.toml`
5. **Code Organization**: Ensure new files follow existing module structure patterns

## Repository Purpose

SURF-API appears to be an internal/enterprise Minecraft plugin framework developed by SLNE Development. It provides:
- Unified APIs for Paper/Bukkit and Velocity servers
- Advanced features like packet manipulation, command handling, and UI components
- Gradle plugin for simplified plugin development
- Code generation tools for boilerplate reduction