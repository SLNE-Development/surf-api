# Flyway Plugin Migrations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let plugins that use `DatabaseApi` version their own schema with Flyway: SQL files, plain JDBC code migrations and Exposed (R2DBC) code migrations, each plugin with its own history table, through a small surf API that keeps Flyway hidden.

**Architecture:** `DatabaseApi` keeps a JDBC `DataSource` alongside the R2DBC pool, built from the same `DatabaseConfig`. A suspend `DatabaseApi.migrate(name) { ... }` DSL collects SQL locations, explicitly registered migrations and an optional package to scan. It then runs Flyway (shaded and relocated) on `Dispatchers.IO` with a per-plugin history table. Surf migration classes are adapted to Flyway's `JavaMigration` internally, so Flyway types never show up in the public API.

**Tech Stack:** Kotlin, Exposed 1.5.1 (R2DBC + exposed-migration-r2dbc), Flyway (core + `flyway-mysql` + `flyway-database-postgresql`), MariaDB/PostgreSQL JDBC drivers, Shadow, JUnit 5, Testcontainers.

**Spec:** none. The user asked to skip it. The requirements below come from the planning Q&A in chat.

## Global Constraints

- Plugins trigger migrations with an explicit call: `databaseApi.migrate(name) { ... }`. `DatabaseApi.create(...)` never migrates.
- DSL shape, as the user wrote it: `migrate { sql("db/migrations"); classes(V2_Foo); classPackage("dev.slne.surf....") }`. Every entry is optional, but at least one must be given. `sql`, `classes` and `classPackage` may each be called more than once.
- History table per plugin: `flyway_schema_history_<name>`. `name` is lowercased and must match `[a-z0-9_]{1,40}`, otherwise `IllegalArgumentException`.
- Baseline: `baselineOnMigrate = true`, default baseline version `"0"`, configurable with `baselineVersion("…")`. All migrations must be idempotent (`CREATE TABLE IF NOT EXISTS`, …). The KDoc and README must say so.
- `cleanDisabled = true` always. A plugin must never be able to wipe a shared database.
- Flyway, the JDBC drivers and their transitive packages are shaded under `dev.slne.surf.database.libs`. No `org.flywaydb` type may appear in `api/surf-database-r2dbc.api`.
- Exposed migrations run in `suspendTransaction` on the existing `R2dbcDatabase`, in a transaction separate from Flyway's history insert.
- JDBC connections come from driver `DataSource` classes (`MariaDbDataSource`, `PGSimpleDataSource`), never from `DriverManager`, because `DriverManager` does not see drivers loaded by isolated plugin classloaders.
- Every public declaration has KDoc in the existing style. Run `./gradlew apiDump` after every public API change and commit `api/surf-database-r2dbc.api`.
- Commit messages follow Conventional Commits, in English.

## Review Focus

1. **A brand-new plugin on a shared, non-empty database.** Baseline at `"0"` must still run that plugin's `V1`. This is why the default is not `"1"`. Pinned in Task 4 (`newPluginOnSharedDatabaseRunsV1`).
2. **Two plugins that both ship `V1__init.sql` on the same database** must not conflict. Pinned in Task 4 (`twoPluginsWithSameVersionsDoNotConflict`).
3. **A plugin jar loaded by an isolated classloader** must still have its SQL found and its scanned classes loaded. The library's Flyway plugins must still be discovered after relocation. Pinned in Task 3 (jar-URL scan) and Task 6 (shaded jar smoke test).
4. **A failing migration** must throw from `migrate()` with the migration version in the message and leave no history row for it (SQL/JDBC). Pinned in Task 4 (`failingSqlMigrationThrowsAndIsNotRecorded`).
5. **A `DatabaseApi` built with the `@TestOnlyDatabaseApi` overload and no JDBC data source** must fail with a clear `IllegalStateException`, not an NPE. Pinned in Task 4 (`migrateWithoutDataSourceFails`).

---

### Task 1: Dependencies and JDBC DataSource on DatabaseApi

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`
- Create: `src/main/kotlin/dev/slne/surf/database/migration/internal/JdbcDataSources.kt`
- Modify: `src/main/kotlin/dev/slne/surf/database/DatabaseApi.kt`
- Test: `src/test/kotlin/dev/slne/surf/database/migration/JdbcDataSourcesTest.kt`

**Interfaces:**
- Produces: `internal object JdbcDataSources { fun create(details: ConnectionDetails): javax.sql.DataSource }`
- Produces: `DatabaseApi` gets `internal val migrationDataSource: DataSource?` (internal constructor param). The config-based `create` fills it. The `@TestOnlyDatabaseApi` overload gets a new last parameter `migrationDataSource: DataSource? = null`.
- Produces: the test dependencies and the `DatabaseContainers` test helper (`src/test/kotlin/dev/slne/surf/database/testing/DatabaseContainers.kt`), which later tasks reuse. It exposes `mariadb(): MariaDBContainer<*>` and `postgres(): PostgreSQLContainer<*>`, singletons started lazily. It also has `fun JdbcDatabaseContainer<*>.toDatabaseApi(): DatabaseApi`, which builds the R2DBC factory from the container's host/port/credentials plus the matching JDBC `DataSource`.

- [ ] **Step 1: Add dependencies.** Pin the latest stable Flyway release (check Maven Central) for `org.flywaydb:flyway-core`, `flyway-mysql` and `flyway-database-postgresql`. Add `org.mariadb.jdbc:mariadb-java-client` and `org.postgresql:postgresql` (latest stable) to `implementation` (new bundle `migration`). Test: `org.junit.jupiter:junit-jupiter`, `org.testcontainers:{junit-jupiter,mariadb,postgresql}`, and `kotlinx-coroutines-test`. Add whatever runtime the tests need from the surf/adventure APIs that `compileOnly` currently provides as `testImplementation`. Add `"org.flywaydb"` and `"org.postgresql"` to `shadedRoots`. Add `mergeServiceFiles()` to the `shadowJar` block.
- [ ] **Step 2: Write the failing test.** `createsMariaDbDataSource`: `ConnectionDetails(databaseType = MARIADB, host = "h", port = 1, database = "d", username = "u", password = "p")` → result `is org.mariadb.jdbc.MariaDbDataSource`, and its URL contains `jdbc:mariadb://h:1/d`. `createsPostgresDataSourceWithSchema`: POSTGRESQL with `schema = "s"` → `is PGSimpleDataSource`, `serverNames == arrayOf("h")`, `portNumbers == intArrayOf(1)`, `databaseName == "d"`, `currentSchema == "s"`. `connectsToMariaDbContainer`: `JdbcDataSources.create(...)` against `DatabaseContainers.mariadb()` → `connection.isValid(5)` is true. Same for Postgres.
- [ ] **Step 3: Run** `./gradlew test --tests '*JdbcDataSourcesTest'`. Expected: FAIL (unresolved `JdbcDataSources`).
- [ ] **Step 4: Implement** `JdbcDataSources.create`. Wire it into the config-based `DatabaseApi.create`, and add the `migrationDataSource` parameter to the test overload and to `createDatabase`.
- [ ] **Step 5: Run** `./gradlew test --tests '*JdbcDataSourcesTest'`. Expected: PASS. Then run `./gradlew apiDump` and check that the diff only adds the new optional parameter.
- [ ] **Step 6: Commit** `build(deps): add flyway and jdbc drivers for plugin migrations` (including `DatabaseApi.kt`, the api file and the tests).

### Task 2: Public migration model and DSL

**Files:**
- Create: `src/main/kotlin/dev/slne/surf/database/migration/SurfMigration.kt`, `MigrationSpec.kt`, `MigrationResult.kt`
- Test: `src/test/kotlin/dev/slne/surf/database/migration/MigrationSpecTest.kt`

**Interfaces:**
- Produces (public, package `dev.slne.surf.database.migration`):
  - `sealed class SurfMigration(val version: String, val description: String) { open val checksum: Int? = null }`
  - `abstract class JdbcMigration(version: String, description: String) : SurfMigration { abstract fun migrate(connection: java.sql.Connection) }`. It runs inside Flyway's JDBC transaction.
  - `abstract class ExposedMigration(version: String, description: String) : SurfMigration { abstract suspend fun R2dbcTransaction.migrate() }`. It runs in `suspendTransaction` on `DatabaseApi.database`.
  - `@DslMarker annotation class MigrationDsl`
  - `@MigrationDsl class MigrationSpec internal constructor(defaultClassLoader: ClassLoader)` with `fun sql(vararg locations: String)`, `fun classes(vararg migrations: SurfMigration)`, `fun classPackage(packageName: String)`, `fun baselineVersion(version: String)` and `fun classLoader(loader: ClassLoader)`
  - `internal fun MigrationSpec.build(name: String): MigrationPlan`, where `internal data class MigrationPlan(val historyTable: String, val sqlLocations: List<String>, val migrations: List<SurfMigration>, val packages: List<String>, val baselineVersion: String, val classLoader: ClassLoader)`
  - `data class MigrationResult(val initialVersion: String?, val targetVersion: String?, val migrationsExecuted: Int)`

- [ ] **Step 1: Write the failing tests.**
  - `historyTableIsDerivedFromName`: `"MyPlugin"` → `historyTable == "flyway_schema_history_myplugin"`.
  - `rejectsInvalidNames`: `"my-plugin"`, `""` and 41×`"a"` each throw `IllegalArgumentException`.
  - `sqlLocationsGetClasspathPrefix`: `sql("db/migrations")` → `"classpath:db/migrations"`, and an existing `classpath:` prefix is not doubled.
  - `defaultsBaselineToZero`: `baselineVersion == "0"`.
  - `emptySpecIsRejected`: `build` with nothing configured throws `IllegalArgumentException`.
  - `duplicateMigrationInstancesAreDeduplicated`: `classes(V1, V1)` → one entry.
- [ ] **Step 2: Run** `./gradlew test --tests '*MigrationSpecTest'`. Expected: FAIL (unresolved).
- [ ] **Step 3: Implement** the files above. The KDoc on `MigrationSpec`, `ExposedMigration` and `baselineVersion` must state the idempotency requirement. On `ExposedMigration` it must also say the migration runs in a separate transaction from the history record.
- [ ] **Step 4: Run** the tests again. Expected: PASS. Then `./gradlew apiDump`.
- [ ] **Step 5: Commit** `feat(migration): add surf migration model and dsl`

### Task 3: Package scanner for migration classes

**Files:**
- Create: `src/main/kotlin/dev/slne/surf/database/migration/internal/MigrationClassScanner.kt`
- Test: `src/test/kotlin/dev/slne/surf/database/migration/MigrationClassScannerTest.kt`, plus fixtures in `src/test/kotlin/dev/slne/surf/database/migration/fixtures/scan/` (`object V2_Object : JdbcMigration`, `class V3_Class : ExposedMigration` with a no-arg constructor, `abstract class Base : JdbcMigration`, `class NotAMigration`, `class V4_NeedsArgs(x: Int) : JdbcMigration`, and a subpackage `nested/V5_Nested`)

**Interfaces:**
- Produces: `internal object MigrationClassScanner { fun scan(classLoader: ClassLoader, packageName: String): List<SurfMigration> }`. It includes subpackages. A Kotlin `object` is resolved through its static `INSTANCE` field, otherwise through the public no-arg constructor. Abstract classes and non-`SurfMigration` classes are skipped. A concrete `SurfMigration` with neither an `INSTANCE` field nor a no-arg constructor throws `IllegalStateException` naming the class. The result is sorted by class name.

- [ ] **Step 1: Write the failing tests.** Move `V4_NeedsArgs` into its own fixture package `fixtures/badscan` so the main scan stays clean.
  - `findsObjectsClassesAndNestedPackages`: scanning `…fixtures.scan` gives the classes `{V2_Object, V3_Class, V5_Nested}`, and the `V2_Object` entry `===` the object instance.
  - `failsOnMigrationWithoutNoArgConstructor`: scanning `…fixtures.badscan` throws `IllegalStateException` whose message contains `V4_NeedsArgs`.
  - `scansClassesInsideJar`: copy the compiled `fixtures/scan` `.class` files into a temp jar (with directory entries), load it with `URLClassLoader(arrayOf(jarUrl), testClassLoader)`, scan it, and get the same three class names, loaded by the jar loader.
  - `unknownPackageReturnsEmpty`.
- [ ] **Step 2: Run** `./gradlew test --tests '*MigrationClassScannerTest'`. Expected: FAIL.
- [ ] **Step 3: Implement.** Use `classLoader.getResources(packagePath)`. For `file:` URLs, walk the directory. For `jar:` URLs, use `(url.openConnection() as JarURLConnection).jarFile` and iterate entries with the package prefix, so a jar without directory entries still works. Load the classes with `Class.forName(name, false, classLoader)`.
- [ ] **Step 4: Run** the tests again. Expected: PASS.
- [ ] **Step 5: Commit** `feat(migration): scan packages for migration classes`

### Task 4: Flyway runner and `DatabaseApi.migrate`

**Files:**
- Create: `src/main/kotlin/dev/slne/surf/database/migration/DatabaseMigrations.kt` (public extension), `migration/internal/FlywayMigrationRunner.kt`, `migration/internal/FlywayJavaMigrationAdapter.kt`, `migration/internal/MigrationClassLoader.kt`
- Test: `src/test/kotlin/dev/slne/surf/database/migration/FlywayMigrationIT.kt`, plus SQL fixtures under `src/test/resources/migrations/{plugin_a,plugin_b,failing}/`

**Interfaces:**
- Consumes: `MigrationSpec.build`, `MigrationPlan` (Task 2), `MigrationClassScanner.scan` (Task 3), `DatabaseApi.migrationDataSource` (Task 1)
- Produces: `suspend fun DatabaseApi.migrate(name: String, block: MigrationSpec.() -> Unit): MigrationResult`. The default class loader is `block.javaClass.classLoader`, because the lambda is compiled into the calling plugin. It runs on `Dispatchers.IO`.
- Produces: `internal class MigrationClassLoader(plugin: ClassLoader) : ClassLoader(DatabaseApi::class.java.classLoader)`. It overrides `getResource`/`getResources` to return the parent's resources plus the plugin's. As a result, Flyway's `ServiceLoader` finds the relocated library plugins and Flyway's classpath scan finds the plugin's SQL.

- [ ] **Step 1: Write the failing integration tests.** Use `@Testcontainers(disabledWithoutDocker = true)` and parameterize over MariaDB and Postgres from `DatabaseContainers`. Use a fresh database or schema per test.
  - `appliesSqlAndCodeMigrationsInOrder`: `plugin_a` has `V1__init.sql` (`CREATE TABLE IF NOT EXISTS a_items`), plus `classes(V2_JdbcInsert, V3_ExposedInsert)` → `migrationsExecuted == 3`, `targetVersion == "3"`, `a_items` has 2 rows, and `flyway_schema_history_plugin_a` exists.
  - `secondRunIsNoOp`: running again → `migrationsExecuted == 0`.
  - `classPackageScanFindsMigrations`: `classPackage("…fixtures.scan")` → `migrationsExecuted == 3`.
  - `twoPluginsWithSameVersionsDoNotConflict`: `plugin_a` and `plugin_b` each have `V1__init.sql` → both succeed, and there are two history tables.
  - `newPluginOnSharedDatabaseRunsV1`: create an unrelated table first, then migrate `plugin_a` → `V1` was executed (`a_items` exists).
  - `existingTablesAreBaselinedAndIdempotentV1Passes`: create `a_items` up front (as `SchemaUtils.create` would have), then migrate → success, and the history has a baseline row `"0"` plus `V1`.
  - `failingSqlMigrationThrowsAndIsNotRecorded`: `failing/V1__broken.sql` → throws an exception whose message contains `"1"`, and the history has no successful `V1` row.
  - `migrateWithoutDataSourceFails`: a `DatabaseApi` from the test overload without a data source → `IllegalStateException` mentioning `migrationDataSource`.
  - `cleanIsDisabled`: assert on the configured `Flyway` (expose `internal fun FlywayMigrationRunner.configure(plan): FluentConfiguration` for the test) → `isCleanDisabled`.
- [ ] **Step 2: Run** `./gradlew test --tests '*FlywayMigrationIT'`. Expected: FAIL (unresolved `migrate`).
- [ ] **Step 3: Implement the adapter.** It implements Flyway's `JavaMigration`, with version/description/checksum from the `SurfMigration`. `canExecuteInTransaction()` returns `true` for `JdbcMigration` and `false` for `ExposedMigration`. `migrate(context)`: a JDBC migration gets `context.connection`; an Exposed migration runs `runBlocking { suspendTransaction(db = api.database) { with(migration) { migrate() } } }`.
- [ ] **Step 4: Implement the runner.** Use `Flyway.configure(MigrationClassLoader(plan.classLoader))` with `.dataSource(api.migrationDataSource ?: error(...))`, `.table(plan.historyTable)`, `.locations(*plan.sqlLocations)` (an empty array when no SQL is configured, so Flyway's default `db/migration` is not used), `.javaMigrations(*adapters)` (explicit migrations plus scanned ones, deduplicated by class), `.baselineOnMigrate(true)`, `.baselineVersion(plan.baselineVersion)` and `.cleanDisabled(true)`. For Postgres, also set `.defaultSchema(schema)`. Map Flyway's `MigrateResult` to `MigrationResult`. Wrap Flyway exceptions in `IllegalStateException("Migration '<name>' failed: …", cause)` so no Flyway type escapes.
- [ ] **Step 5: Run** the tests again. Expected: PASS on both databases. Then `./gradlew apiDump` and check that the api file has no `flywaydb` (`grep -c flywaydb api/surf-database-r2dbc.api` → `0`).
- [ ] **Step 6: Commit** `feat(migration): run plugin migrations with flyway`

### Task 5: SQL script generation from Exposed tables

**Files:**
- Modify: `src/main/kotlin/dev/slne/surf/database/migration/DatabaseMigrations.kt`
- Test: `src/test/kotlin/dev/slne/surf/database/migration/GenerateMigrationScriptIT.kt`

**Interfaces:**
- Produces: `suspend fun DatabaseApi.generateMigrationScript(vararg tables: Table, scriptDirectory: Path, scriptName: String): Path`. It wraps the R2DBC `MigrationUtils.generateMigrationScript` from `exposed-migration-r2dbc` (check its exact signature in the 1.5.1 sources) in `suspendTransaction(db = database)`. It returns the written file. The KDoc warns that the generated statements are not idempotent and must be edited before they ship.

- [ ] **Step 1: Write the failing test.** `generatesCreateStatementForMissingTable`: an empty database and `object Items : LongIdTable("items")` → the file `V2__items.sql` exists in a temp dir and contains `CREATE TABLE` and `items` (case-insensitive). Run it against both containers.
- [ ] **Step 2: Run** `./gradlew test --tests '*GenerateMigrationScriptIT'`. Expected: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** the test again. Expected: PASS. Then `./gradlew apiDump`.
- [ ] **Step 5: Commit** `feat(migration): generate migration scripts from exposed tables`

### Task 6: Shaded jar smoke test

**Files:**
- Modify: `build.gradle.kts`
- Create: `src/shadowTest/kotlin/dev/slne/surf/database/ShadedFlywayTest.kt`, `src/shadowTest/resources/db/smoke/V1__smoke.sql`

**Interfaces:**
- Produces: the source set `shadowTest` and the task `shadowTest` (type `Test`). Its runtime classpath is the `shadowJar` output + the `shadowTest` output + test runtime libraries (Testcontainers, JUnit, coroutines, slf4j, the surf APIs), without `sourceSets.main.output` and without unshaded Flyway or the drivers. `check` depends on `shadowTest`.

- [ ] **Step 1: Write the failing test.** `shadedFlywayFindsDatabasePluginsAndPluginResources`: put `V1__smoke.sql` in a separate temp directory and load it through a `URLClassLoader` whose parent is the platform classloader. This simulates an isolated plugin loader that cannot see the shaded jar. Call `migrate("smoke") { sql("db/smoke"); classLoader(isolatedLoader) }` against MariaDB and Postgres → `migrationsExecuted == 1`. Also assert `Class.forName("org.flywaydb.core.Flyway")` throws `ClassNotFoundException`, which proves the classpath is the relocated one.
- [ ] **Step 2: Run** `./gradlew shadowTest`. Expected: FAIL, either because the task doesn't exist yet or because of `No database found to handle jdbc:…` if the service files aren't merged or relocated.
- [ ] **Step 3: Implement** the source set and the task. If Flyway's plugins are still not found, check that `META-INF/services/dev.slne.surf.database.libs.org.flywaydb.core.extensibility.Plugin` exists in the jar with relocated class names, and fix the shadow configuration until it does.
- [ ] **Step 4: Run** `./gradlew shadowTest`. Expected: PASS.
- [ ] **Step 5: Commit** `test(migration): verify flyway works from the shaded jar`

### Task 7: Documentation and version

**Files:**
- Modify: `README.md`, `gradle.properties`

- [ ] **Step 1:** Add a "Migrations" section to the README after "Service Pattern". It covers the `migrate(name) { sql(...); classes(...); classPackage(...) }` example inside `connect()`, file naming (`V<n>__<desc>.sql`), the history table per plugin, the baseline `"0"` plus the idempotency rule (with a `CREATE TABLE IF NOT EXISTS` example), `JdbcMigration` vs `ExposedMigration` (the Exposed one is not in the same transaction as the history record), `generateMigrationScript` as a dev helper, and the fact that `clean` is disabled. Also update the service-pattern example to call `migrate` instead of `SchemaUtils.create`.
- [ ] **Step 2:** Bump `version=2.3.5` → `version=2.4.0`.
- [ ] **Step 3: Run** `./gradlew build`. Expected: BUILD SUCCESSFUL (tests, `shadowTest` and `apiCheck` all pass).
- [ ] **Step 4: Commit** `docs(migration): document flyway plugin migrations`
