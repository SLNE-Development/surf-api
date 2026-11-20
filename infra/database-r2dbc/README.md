# surf-database-r2dbc

`surf-database-r2dbc` is an R2DBC provider for **JetBrains Exposed**, enabling non-blocking database
operations in surf microservices.

It provides:

- Integration of Exposed DSL with R2DBC
- Non-blocking, reactive database operations
- Kotlin coroutines support
- Configuration-based connection pool management
- Flyway migrations per service
- MariaDB and PostgreSQL support via R2DBC

The library is built on top of **JetBrains Exposed**, **R2DBC**, and **Kotlin coroutines**.

---

## Installation

The library is published as `dev.slne.surf.api:surf-api-database-r2dbc` in the surf-api version. It serves
microservices: the Gradle plugin `dev.slne.surf.api.gradle.microservice` adds it to `implementation` unless
the microservice disables the database (see [the surf-microservice README](../microservice/README.md#configure-the-build)). Paper, Velocity and Minestom
plugins do not use it.

The published jar bundles Exposed, R2DBC, the MariaDB and PostgreSQL drivers, Netty and Flyway, relocated
below `dev.slne.surf.database.libs`. Import Exposed from the relocated packages, for example:

```kotlin
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
```

---

## Concepts

### Spring auto-configuration

`SurfDatabaseAutoConfiguration` (package `dev.slne.surf.database.spring`) provides the database as
Spring beans. The library lists it in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`,
so a Spring Boot application picks it up without further setup. The `surf-microservice-database`
module builds on it for microservices.

The configuration provides these beans:

| Bean | Purpose |
|---|---|
| `DatabaseConfig` | The connection and pool settings, read from `<surf.config-directory>/database.yml`. The application can define its own bean, which replaces this one. The start validates it and uses it as it is, without the `SURF_DATABASE_*` overrides. |
| `SurfMigrationRunner` | Runs every `MigrationSet` bean while the context starts, and runs further migrations on demand. |
| `R2dbcDatabase` | The Exposed database, created after the migrations ran. |

The connection pool and the JDBC data source of the migrations are not beans of their own, so
neither Spring Boot's R2DBC and Flyway auto-configurations nor application code pick them up.

Lifecycle:

1. The context reads `database.yml` and opens the connection pool.
2. `SurfMigrationRunner` runs every `MigrationSet` bean in `@Order` order. A failed migration fails
   the start and releases the pool.
3. The `R2dbcDatabase` bean becomes available. Every bean that injects it sees the migrated schema.
4. When the context closes, the library unregisters the database from Exposed and disposes the pool and
   its event loop.

Inject the database like any other bean and run queries with Exposed:

```kotlin
@Service
class UserService(private val database: R2dbcDatabase) {

    suspend fun findUser(id: UUID): User? = suspendTransaction(database) {
        UsersTable.selectAll().where { UsersTable.id eq id }.singleOrNull()?.toUser()
    }
}
```

### Directory of `database.yml`

The property `surf.config-directory` names the directory that contains `database.yml`. It defaults to
`config`, and the environment variable `SURF_CONFIG_DIRECTORY` sets it. The pool is named after
`spring.application.name`, or `surf` when the property is not set. This name appears in logs and
monitoring.

A microservice sets `surf.config-directory` itself, from `SURF_MICROSERVICE_CONFIG_DIR` and ahead of every other
property source. In a microservice, `SURF_CONFIG_DIRECTORY` has no effect: set `SURF_MICROSERVICE_CONFIG_DIR`
instead. A microservice also publishes its name as `spring.application.name`.

### Customizing Exposed's configuration

Every `SurfDatabaseConfigurer` bean customizes Exposed's `R2dbcDatabaseConfig` in `@Order` order,
after the library's defaults are set:

```kotlin
@Bean
fun statementLimits() = SurfDatabaseConfigurer { it.defaultFetchSize = 500 }
```

Replacing `typeMapping` drops the library's UTC mapping of Exposed's `timestamp()` columns (see
[Time zones](#time-zones)).

The library also sets Exposed's `defaultMaxAttempts` to `1`. A transaction therefore runs its block once
and never repeats on its own; retry explicitly with
[`retryingTransaction`](#retrying-transactions). Configurers run after this setting, so a bean can restore
Exposed's default of 3 attempts:

```kotlin
@Bean
fun exposedRetries() = SurfDatabaseConfigurer { it.defaultMaxAttempts = 3 }
```

---

## Migrations

Schema changes are applied with [Flyway](https://flywaydb.org/). Describe the migrations of a service
or a library as a `MigrationSet` bean. A microservice gets a set for its own migrations by convention
(see [Migrations in surf-microservice](../microservice/README.md#migrations)); declare further sets for
other migrations. The `SurfMigrationRunner` runs every set once while the context starts, in `@Order`
order, before any bean receives the `R2dbcDatabase`. The migrations of a set run on `Dispatchers.IO`; its
`beforeMigrate` and `afterMigrate` hooks run on the thread that starts the context.

```kotlin
@Component
class ItemMigrations : MigrationSet {

    override val name = "items"

    override fun MigrationSpec.describe() {
        sql("db/migrations")                                   // V<n>__<desc>.sql files on the classpath
        classes(V2_BackfillItems)                              // code migrations
        classNames("dev.slne.surf.items.migrations.V3_Seed")   // code migrations by binary class name
        classPackage("dev.slne.surf.items.migrations")         // scan a package for code migrations
    }
}
```

At least one of `sql`, `classes`, `classNames` or `classPackage` is required; each may be given more than once.
`name` must match `[a-z0-9_]{1,40}` (it is lowercased). `classNames` loads exactly the named classes, each a
Kotlin `object` or a class with a public constructor without parameters, and touches no other class of their
package. `classPackage` scans a package and fails when it has no migration classes, unless you pass
`required = false`.

A set can override `beforeMigrate(database)` and `afterMigrate(database)` to check the server or the
migrated tables. A set must not inject the `R2dbcDatabase` bean, which exists only after every set
ran; these two functions receive the database instead.

To run migrations later, inject `SurfMigrationRunner` and call `migrate`. The function is a suspend
function:

```kotlin
val result = runner.migrate("items") {
    sql("db/migrations")
}
logger.info("Applied ${result.migrationsExecuted} migration(s), schema was at ${result.initialVersion}")
```

`result.targetVersion` is the version of the last migration executed in this run, so it is `null`
when there was nothing to do; `result.initialVersion` is `null` the first time a set migrates
an empty schema.

**Do not call `migrate()` inside an open transaction.** Flyway uses its own JDBC connection, and on
MariaDB the metadata locks held by your open transaction can block Flyway's DDL indefinitely.

### SQL files

SQL migrations are classpath resources named `V<version>__<description>.sql`, for example
`db/migrations/V1__create_users.sql` and `db/migrations/V2__add_items.sql`. They are looked up through
the class loader of the `MigrationSet` (or of the lambda passed to `SurfMigrationRunner.migrate`) by
default, or the one you set with `classLoader(...)`.

A location that cannot be found fails `migrate()`, and so does a SQL file whose name does not follow
the convention (for example `V1_init.sql` with a single underscore), so a typo never silently skips
migrations.

### Vendor-specific SQL files

`{vendor}` in a location is replaced by the vendor of the database: `mariadb` or `postgresql`, the
lowercase names of `DatabaseType` (also available as `database.databaseType.vendor`). A set that
supports both databases ships one folder per vendor and registers a single location:

```kotlin
override fun MigrationSpec.describe() {
    sql("db/migrations/{vendor}")   // db/migrations/mariadb or db/migrations/postgresql
}
```

Both folders share one history table, so keep their versions in step. If the folder of the active
vendor is missing, `migrate()` fails like for any other missing location.

### History table per set

Each `name` gets its own history table `flyway_schema_history_<name>`, so sets that share a database
do not interfere with each other, even if they use the same versions.

### Baseline and idempotency

A database without history for `name` is baselined at version `"0"` (change it with
`baselineVersion(...)`), so a brand-new set still runs its `V1` on a shared, non-empty database.
Because the database may already contain tables created before migrations were introduced, **all
migrations must be idempotent**:

```sql
CREATE TABLE IF NOT EXISTS users (
    id   BINARY(16)   NOT NULL PRIMARY KEY,
    name VARCHAR(64)  NOT NULL
);
```

### Code migrations

For changes that SQL cannot express, extend one of:

- `JdbcMigration` runs on plain JDBC inside the transaction of the migration run. Do not commit or
  close the connection yourself.
- `ExposedMigration` runs in a `suspendTransaction` using Exposed over R2DBC. This transaction is
  **not** the one that records the migration in the history table, so if the process stops between the
  two, the migration runs again on the next start.

```kotlin
object V2_BackfillItems : ExposedMigration("2", "backfill item names") {
    override suspend fun R2dbcTransaction.migrate() {
        // Plain SQL, so the migration keeps working when ItemsTable changes later. Idempotent.
        exec("UPDATE items SET display_name = name WHERE display_name IS NULL")
    }
}
```

### Failed migrations

A failed migration makes `migrate()` throw with the migration's version and description in the
message. Whether a row is left in `flyway_schema_history_<name>` depends on the database:

| Migration          | MariaDB            | PostgreSQL                                       |
|--------------------|--------------------|--------------------------------------------------|
| SQL file           | recorded as failed | rolled back, no row                              |
| `JdbcMigration`    | recorded as failed | rolled back, no row                              |
| `ExposedMigration` | recorded as failed | its own transaction is rolled back, no row       |

MariaDB cannot roll back DDL, so Flyway records **every** failed migration there as failed
(`success = false`), whether it changed the schema, only data, or nothing at all. While such a row
exists, every later `migrate()` for that set fails. There is no repair API: fix the migration, undo
or finish what it partially did, then delete the failed row manually from
`flyway_schema_history_<name>`. This is another reason migrations must be idempotent: after fixing the
problem the migration runs again against a possibly partially changed schema.

### Generating a script (development helper)

`R2dbcDatabase.generateMigrationScript(...)` compares Exposed tables with the database and writes the
difference to a SQL file you can use as the next migration:

```kotlin
database.generateMigrationScript(
    ItemsTable,
    scriptDirectory = Path.of("src/main/resources/db/migrations"),
    scriptName = "V2__items"
)
```

Run it against a database that does not yet contain the change. The generated statements are **not
idempotent** (for example a plain `CREATE TABLE`); edit the file before shipping it.

### Safety

`clean` is disabled, so Flyway never drops objects from your database.

---

## Configuration

### Database Config File

The `DatabaseConfig` bean loads its configuration from the file `database.yml` in the directory named by
`surf.config-directory`. The surf config API writes the file with its defaults if it does not exist.

Example `database.yml` (keys are kebab-case; camelCase keys are silently ignored):

```yaml
credentials:
  database-type: MARIADB
  host: localhost
  port: 3306
  username: myuser
  password: mypassword
  database: mydb

pool:
  sizing:
    initial-size: 5
    min-idle: 5
    max-size: 20

  timeouts:
    max-acquire-time-millis: 30000
    max-create-connection-time-millis: 10000
    max-idle-time-millis: 600000
    max-life-time-millis: 1800000
    max-validation-time-millis: 5000
    lock-timeout-millis: 10000

log-level: DEBUG
```

### Runtime environment variables

The `DatabaseConfig` bean first loads `database.yml`, then applies environment-variable
overrides before the configuration creates the connection factory and pool. Overrides apply to the
loaded value only and are never written to the file:

`environment > database.yml > built-in default`

A missing variable preserves its YAML/default value. A present but malformed value fails startup and
identifies the variable. Database types and log levels are parsed case-insensitively. Pool timeout
values retain the existing millisecond semantics, including `-1` where R2DBC Pool uses it to disable
a timeout.

An acquire that times out is not retried, so `max-acquire-time-millis` is the longest a statement waits for a
connection. Keep it at or below the RPC timeout of the calls that use the pool (5 s for core surf-nats calls).
`initial-size` is also the smallest size the pool shrinks to; `min-idle` only raises it.

| Environment variable                                   | YAML option                                   | Built-in default                                   |
|--------------------------------------------------------|-----------------------------------------------|----------------------------------------------------|
| `SURF_DATABASE_LOG_LEVEL`                              | `log-level`                                   | `DEBUG`                                            |
| `SURF_DATABASE_TYPE`                                   | `credentials.database-type`                   | `MARIADB`                                          |
| `SURF_DATABASE_SCHEMA`                                 | `credentials.schema`                          | `public` (PostgreSQL only)                         |
| `SURF_DATABASE_HOST`                                   | `credentials.host`                            | `localhost`                                        |
| `SURF_DATABASE_PORT`                                   | `credentials.port`                            | `3306` (MariaDB) or `5432` (PostgreSQL); `1..65535` |
| `SURF_DATABASE_NAME`                                   | `credentials.database`                        | `database`                                         |
| `SURF_DATABASE_USERNAME`                               | `credentials.username`                        | `root`                                             |
| `SURF_DATABASE_PASSWORD`                               | `credentials.password`                        | existing YAML/default value; configure as a secret |
| `SURF_DATABASE_POOL_INITIAL_SIZE`                      | `pool.sizing.initial-size`                    | `10`                                               |
| `SURF_DATABASE_POOL_MIN_IDLE`                          | `pool.sizing.min-idle`                        | `0`                                                |
| `SURF_DATABASE_POOL_MAX_SIZE`                          | `pool.sizing.max-size`                        | `10`                                               |
| `SURF_DATABASE_POOL_MAX_ACQUIRE_TIME_MILLIS`           | `pool.timeouts.max-acquire-time-millis`       | `5000`                                             |
| `SURF_DATABASE_POOL_MAX_CREATE_CONNECTION_TIME_MILLIS` | `pool.timeouts.max-create-connection-time-millis` | `5000`                                             |
| `SURF_DATABASE_POOL_MAX_VALIDATION_TIME_MILLIS`        | `pool.timeouts.max-validation-time-millis`    | `-1`                                               |
| `SURF_DATABASE_POOL_MAX_IDLE_TIME_MILLIS`              | `pool.timeouts.max-idle-time-millis`          | `60000`                                            |
| `SURF_DATABASE_POOL_MAX_LIFE_TIME_MILLIS`              | `pool.timeouts.max-life-time-millis`          | `1800000`                                          |
| `SURF_DATABASE_POOL_LOCK_TIMEOUT_MILLIS`               | `pool.timeouts.lock-timeout-millis`           | `10000`; `1..2147483647`, MariaDB rounds up to whole seconds |

MariaDB example:

```dotenv
SURF_DATABASE_TYPE=mariadb
SURF_DATABASE_HOST=mariadb.internal
SURF_DATABASE_PORT=3306
SURF_DATABASE_NAME=surf
SURF_DATABASE_USERNAME=surf-service
SURF_DATABASE_PASSWORD=
SURF_DATABASE_POOL_MAX_SIZE=20
```

PostgreSQL example:

```dotenv
SURF_DATABASE_TYPE=postgresql
SURF_DATABASE_SCHEMA=application
SURF_DATABASE_HOST=postgres.internal
SURF_DATABASE_PORT=5432
SURF_DATABASE_NAME=surf
SURF_DATABASE_USERNAME=surf-service
SURF_DATABASE_PASSWORD=
SURF_DATABASE_POOL_MAX_SIZE=20
```

For Coolify, add these variables to the application service and point the host at the external
database service. Mark `SURF_DATABASE_PASSWORD` as a secret. Do not commit real database credentials
to `database.yml`, Dockerfiles, `.env` files, or source control. The application image does not
install or run the database server.

### Defining the configuration yourself

The `DatabaseConfig` bean is conditional on a missing bean. To read the settings from elsewhere,
define your own bean and the configuration uses it:

```kotlin
@Bean
fun databaseConfig(configApi: SurfConfigApi): DatabaseConfig =
    DatabaseConfig.load(configApi, Path("config/shared"), "storage.yml")
```

`DatabaseConfig.load` applies the environment overrides, too.

---

## Database Operations

`surf-database-r2dbc` is a **provider**, not a query API. All database operations are performed
using **JetBrains Exposed**.

Refer to the official Exposed documentation:

- [Exposed Documentation](https://www.jetbrains.com/help/exposed/dsl-crud-operations.html)

### Example: Simple Query

```kotlin
suspend fun findUserById(database: R2dbcDatabase, id: UUID): User? = suspendTransaction(database) {
    UsersTable
        .selectAll()
        .where { UsersTable.id eq id }
        .singleOrNull()
        ?.toUser()
}
```

### Retrying transactions

`R2dbcDatabase.retryingTransaction` runs a block in a new top-level transaction and runs it again
when the database rolls it back for a deadlock or a serialization failure. Any other failure
reaches the caller after one attempt:

```kotlin
database.retryingTransaction(maxAttempts = 3) {
    Accounts.update({ Accounts.id eq from }) { it[Accounts.balance] = Accounts.balance - amount }
    Accounts.update({ Accounts.id eq to }) { it[Accounts.balance] = Accounts.balance + amount }
}
```

The block can run up to `maxAttempts` times, so side effects outside the database must tolerate a
repeat. The function fails inside an open transaction, whose rollback it cannot repeat.
`isTransientConflict()` in `dev.slne.surf.database.utils` tells such a conflict apart for your own
retry logic.

### Query helpers

`dev.slne.surf.database.query` adds SQL that Exposed does not offer, rendered for PostgreSQL and
MariaDB:

| Helper | Purpose |
|---|---|
| `allOf(...)`, `anyOf(...)` | Combine conditions with `AND` / `OR`, skipping `null`s. With no condition left they return `TRUE` / `FALSE`; `allOfOrNull` and `anyOfOrNull` return `null` instead. |
| `column containsIgnoringCase text` | Case-insensitive substring match. `%`, `_` and `\` in `text` match only themselves. |
| `column likeIgnoringCase pattern` | Case-insensitive `LIKE` with an Exposed `LikePattern`. |
| `column + duration`, `column - duration` | Shift a timestamp by a `kotlin.time.Duration`, exact to the microsecond. |
| `column.plusDays(n)`, `column.minusDays(n)` | Shift a date or timestamp by calendar days; a date stays a date. |
| `unionAllOf(queries)` | Combine queries with `UNION ALL` for ordering and paging across tables; a single query is returned as a copy. |
| `table.selfReferencingChain(parent, start, maxDepth)` | Match a row and its ancestors through a recursive CTE. |

```kotlin
val banned = BansTable.selectAll().where {
    allOf(
        BansTable.createdAt greaterEq (CurrentOffsetDateTime() - 30.days),
        query?.let { BansTable.reason containsIgnoringCase it },
        playerId?.let { BansTable.player eq it },
    )
}
```

The date helpers keep the column type, so the shifted expression compares with and reads back as
the column's own values. The case-insensitive helpers render `LOWER(value) LIKE LOWER(pattern)`;
the column collation still applies, and MariaDB's default `*_ai_ci` collations also ignore accents.
For `unionAllOf`, select the same aliases in every query, order by `alias.aliasOnlyExpression()` and
read rows through the first query's expressions.

Some tasks need no helper because Exposed covers them:

* **Calendar days in UTC:** `column.date()` from `exposed-java-time` returns the UTC day of every
  time column, because all connections run in UTC (see [Time zones](#time-zones)).
* **Insert if absent:** `insertIgnore` renders `ON CONFLICT DO NOTHING` on PostgreSQL and
  `INSERT IGNORE` on MariaDB, and keeps the transaction usable. On MariaDB, `IGNORE` also skips
  rows that violate a foreign key, which fail on PostgreSQL.

To react to a duplicate key of a plain insert, check the failure with
`dev.slne.surf.database.utils.isDuplicateKeyViolation()`. It matches unique and primary key
violations only, unlike `isDataIntegrityViolation()`. On PostgreSQL the failed statement aborts the
transaction, so retry in a new one.

### Time zones

Every connection the library opens, R2DBC and Flyway's JDBC, runs with the session time zone UTC,
whatever the JVM default zone and the server zone are. Instants are stored as UTC, wall-clock
columns hold UTC wall-clock time, and SQL migrations interpret literals without offset as UTC.

| Column | Created as (PostgreSQL / MariaDB) | Reads back |
|---|---|---|
| `offsetDateTime()`, `zonedDateTime()` | `TIMESTAMP` / `TIMESTAMP(6)`; also work on `timestamptz` / `DATETIME(6)` | the exact instant, in the JVM default zone |
| Exposed `timestamp()` | `TIMESTAMP` / `TIMESTAMP(6)`; also works on `timestamptz` / `DATETIME(6)` | the instant (see the limits below) |
| Exposed `timestampWithTimeZone()` | `TIMESTAMP WITH TIME ZONE` / not supported by Exposed | the exact instant |
| Exposed `datetime()` | `TIMESTAMP` / `DATETIME(6)` | the stored wall-clock time; a `timestamptz` column as UTC wall-clock time |

Database-generated values follow the same rule: `CurrentOffsetDateTime`, `CurrentTimestamp`,
`CurrentTimestampWithTimeZone` and the `created_at`/`updated_at` columns of `AuditableTable` read as
the current instant. `CurrentDateTime` yields the current UTC wall-clock time, so
compare it with `LocalDateTime.now(ZoneOffset.UTC)`, not `LocalDateTime.now()`.

Exposed's `timestamp()` and `datetime()` convert values through the JVM default zone before the
library sees them, so three cases still depend on that zone:

* `timestamp()`: an instant in the repeated hour of a daylight saving fall-back in the JVM default
  zone is stored one hour early;
* `datetime()`: a wall-clock value in the hour a daylight saving spring-forward skips in the JVM
  default zone, such as `02:30` on that day in `Europe/Berlin`, is stored one hour late;
* `timestampLiteral(...)` and `.default(instant)` render wall-clock time in the JVM default zone.

If these cases matter, use `offsetDateTime()` for instants or run the JVM in UTC. The library
handles Exposed's columns with an R2DBC type mapper; a `SurfDatabaseConfigurer` that replaces
`typeMapping` removes it.

In a `JdbcMigration`, the JDBC drivers convert some parameter types through the JVM default zone,
although the session zone is UTC:

* pgjdbc converts a `java.sql.Timestamp` bound to a `timestamp` column;
* MariaDB Connector/J converts a `java.sql.Timestamp` and an `OffsetDateTime`, whatever its offset,
  for `DATETIME` and `TIMESTAMP` columns alike. Its option `preserveInstants=true` would prevent
  this; the library does not set it.

Bind instants as UTC wall-clock time instead, which stores the exact instant in every time column
on both vendors:

```kotlin
statement.setObject(index, LocalDateTime.ofInstant(instant, ZoneOffset.UTC))
```

---

## Idempotent operations

`AtomicOperations` in `dev.slne.surf.database.reliability.operation` makes message handlers safe to retry. It
does not depend on any messaging library. All services on a database share one table and are
separated by an *owner*, usually the service name.

Register `ReliabilityMigrationSet` from `dev.slne.surf.database.reliability` as a bean wherever you use `AtomicOperations`, also when the
application does not migrate its own schema. The migrations belong to the library and keep their own
history in `flyway_schema_history_surf_reliability`; on MariaDB the set also checks for MariaDB 10.6
or newer and InnoDB:

```kotlin
@Bean
fun reliabilityMigrations() = ReliabilityMigrationSet()
```

`AtomicOperations.execute` runs a block at most once per `OperationKey(owner, operation,
idempotencyKey)`. The key is claimed, the block runs, and its reply is stored, all in one
`READ COMMITTED` transaction. A repeated call with a committed key returns the stored reply without
running the block:

```kotlin
val operations = AtomicOperations(database)

val reply = operations.execute(OperationKey("billing", "CreateInvoice", messageId), sha256(request)) {
    InvoiceTable.insert { /* ... */ }
    StoredReply("application/protobuf", encode(response))
}
```

* A concurrent call with the same key waits for the first one and returns its reply, or runs the
  block if the first one rolls back. It waits at most `lockTimeout` (default 10 s, at least 1 ms and
  at most `Int.MAX_VALUE` ms), then fails with
  `AtomicOperationInProgressException` without running the block.
* If the process dies before the commit, nothing is stored and the next call runs the block again.
* The same key with a different request fingerprint fails with
  `AtomicOperationConflictException`.
* Replies are kept for `retention` (default 14 days); a call after that runs the block again.
  `deleteExpired()` removes expired rows in batches.
* After a deadlock or serialization failure the whole transaction is retried, so the block may run
  more than once; keep side effects outside the database idempotent.
* `execute` owns its transaction: calling it inside `suspendTransaction` fails.

All processes sharing the table need synchronized clocks: retention uses the application clock, not
the database clock.

In a microservice, `surf-api-microservice-reliability` already provides the `AtomicOperations` bean for its
`@NatsTransactional` services, so the sample above is for code outside that module. A bean that replaces the provided
one must use the microservice's own `R2dbcDatabase` and a `retention` of at least
`NatsPersistentLimits.REDELIVERY_HORIZON`; see
[the surf-microservice README](../microservice/README.md#idempotent-handlers-and-events-after-the-commit).

---

## Testing

Start the auto-configuration in a plain Spring context against a real database, for example a
Testcontainers server. A `DatabaseConfig` bean replaces the one read from `database.yml`, so no file
is needed. Closing the context releases the pool:

```kotlin
@Testcontainers
class ItemRepositoryTest {

    companion object {
        @Container
        val mariaDb = MariaDBContainer("mariadb:12.3")
    }

    @Test
    fun `reads migrated items`() {
        val credentials = ConnectionDetails(
            databaseType = DatabaseType.MARIADB,
            host = mariaDb.host,
            port = IntOr.Default(mariaDb.getMappedPort(3306)),
            database = mariaDb.databaseName,
            username = "root",
            password = mariaDb.password,
        )

        AnnotationConfigApplicationContext().apply {
            registerBean<DatabaseConfig> { DatabaseConfig(credentials = credentials) }
            registerBean<ItemMigrations>()
            register(SurfDatabaseAutoConfiguration::class.java)
            refresh()
        }.use { context ->
            val database = context.getBean<R2dbcDatabase>()
            // run queries with suspendTransaction(database) { ... }
        }
    }
}
```

The library connects every R2DBC and JDBC connection with the session time zone UTC (see
[Time zones](#time-zones)). A test that builds its own `ConnectionFactory` must set it too (option
`timezone` = `UTC` for MariaDB, `timeZone("UTC")` for PostgreSQL); otherwise the time columns store
and read shifted values.

---

## Supported Databases

The auto-configuration supports **MariaDB** and **PostgreSQL** through their R2DBC
drivers. Set `credentials.database-type` or `SURF_DATABASE_TYPE` to select the driver.
`credentials.schema` and `SURF_DATABASE_SCHEMA` apply only to PostgreSQL and are safely ignored for
MariaDB. The [idempotent operations](#idempotent-operations) need MariaDB 10.6
or newer with InnoDB. The integration tests run against MariaDB 12.3 and PostgreSQL 18.

---

## Guarantees & Non-Guarantees

Guaranteed:

* Non-blocking database operations
* Connection pooling with configurable sizing and timeouts
* Integration with Exposed's DSL and type-safe queries
* Proper resource cleanup when the Spring context closes

Not guaranteed:

* Support for databases other than MariaDB and PostgreSQL without custom setup
* Schema migrations without a `MigrationSet` bean or a call to `SurfMigrationRunner.migrate`
* Cross-database transactions

---

## Common Pitfalls

### 1. Closing the pool yourself

The Spring context owns the connection pool and disposes it when the context closes. Do not close
the pool or unregister the database from Exposed in application code, and make sure the application
closes the context on shutdown. Spring Boot does this through its shutdown hook.

---

### 2. Injecting the `R2dbcDatabase` into a `MigrationSet`

The `R2dbcDatabase` bean exists only after every `MigrationSet` ran, so a set must not depend on it.
Use the `database` parameter of `beforeMigrate` and `afterMigrate`, or inject `SurfMigrationRunner`
into a bean that runs after the start.

---

### 3. Using blocking JDBC instead of R2DBC transactions

Exposed supports both JDBC and R2DBC. This library provides **R2DBC only**.

Always use:

```kotlin
suspendTransaction(database) {
    // queries here
}
```

Do **not** use:

```kotlin
transaction {
    // This is blocking JDBC, not R2DBC
}
```

---

### 4. Calling `migrate` from a transaction

`SurfMigrationRunner.migrate` is a suspend function, and it must run outside every transaction.
Flyway uses its own JDBC connection, so a metadata lock held by your open transaction can block
its DDL indefinitely:

```kotlin
// BAD
suspendTransaction(database) {
    runner.migrate("items") { sql("db/migrations") }
}
```

```kotlin
// GOOD
runner.migrate("items") { sql("db/migrations") }
```

---

### 5. Not understanding connection pool limits

The connection pool has a maximum size defined in `database.yml`.

If all connections are in use, new transactions will wait up to `max-acquire-time-millis` before
failing.

Monitor connection usage and adjust pool sizing if needed:

```yaml
pool:
  sizing:
    max-size: 50  # Increase if needed
```

---

### 6. Opening a second pool

The auto-configuration creates exactly one pool per context. Do not open another pool next to it
for the same database through Spring Boot's R2DBC auto-configuration or your own factory: both
pools count against the connection limit of the server, and `max-size` no longer bounds the total.

---

## Upgrading data from surf-database-r2dbc before 2.4.0

This section applies to databases that the standalone library `surf-database-r2dbc` wrote before
its version 2.4.0. For the move from surf-api 3.x to 4.0, including the removed `DatabaseApi`, see
[MIGRATION-4.0.md](../../MIGRATION-4.0.md#14-microservices-became-spring-boot-applications).

The shaded jar merges the `META-INF/services` files of all bundled libraries (Flyway needs this to
find its database plugins). Service registrations of the relocated libraries that had no effect
before 2.4.0 are therefore active, for example Exposed's R2DBC `TypeMapper`s and the plugins of the
R2DBC MariaDB (`AuthenticationPlugin`) and PostgreSQL (`Extension`) drivers. If you worked around
missing type mappings or driver plugins, check whether the workaround is still needed.

Every connection runs with the session time zone UTC, and Exposed's `timestamp()` and `datetime()`
columns are exchanged as UTC (see [Time zones](#time-zones)). Before 2.4.0, the PostgreSQL session
used the JVM default zone (`J` below) and the MariaDB session the server zone (`S` below).
**If both were UTC, nothing changes.** Otherwise existing rows read as follows; nothing is converted
automatically. *App values* are values the application wrote, *defaults* are values of a
`CURRENT_TIMESTAMP` column default. Values the application wrote as a database expression, such as
`it[c] = CurrentTimestamp` or `it[c] = CurrentDateTime`, were computed by the database in the
session zone exactly like a default, so they count as defaults here.

| Vendor | Column | Existing rows after the upgrade | Fix |
|---|---|---|---|
| PostgreSQL | `offsetDateTime()`/`zonedDateTime()` on `timestamp` (Exposed DDL) | Unchanged. | – |
| PostgreSQL | `offsetDateTime()`/`zonedDateTime()` on `timestamptz` | App values were stored shifted by `J` and stay shifted. Defaults are correct. | A on app values |
| PostgreSQL | `timestamp()` on `timestamp` (Exposed DDL) | App values and defaults hold wall-clock time in `J` and read shifted by `J`'s offset. | A |
| PostgreSQL | `timestamp()` on `timestamptz`, `timestampWithTimeZone()` | Unchanged. | – |
| PostgreSQL | `datetime()` on `timestamp` (Exposed DDL) | App values unchanged. Defaults keep wall-clock time in `J`; new defaults hold UTC wall-clock time. | A on defaults, to align them |
| PostgreSQL | `datetime()` on `timestamptz` | App values were stored as instants in `J` and now read as UTC wall-clock time, shifted by `J`'s offset. Defaults now read as UTC wall-clock time. | A on app values |
| MariaDB | `offsetDateTime()`/`zonedDateTime()` on `TIMESTAMP` (Exposed DDL) | App values were stored shifted by `S` and now read shifted. Defaults read correctly. | B on app values |
| MariaDB | `timestamp()` on `TIMESTAMP` (Exposed DDL) | App values read shifted by the difference of `J` and `S`. Defaults read correctly. | C on app values |
| MariaDB | `offsetDateTime()`/`zonedDateTime()` on `DATETIME` | App values unchanged. Defaults hold wall-clock time in `S` and read shifted by `S`'s offset. | D on defaults |
| MariaDB | `timestamp()` on `DATETIME` | App values hold wall-clock time in `J` and read shifted by `J`'s offset. Defaults hold wall-clock time in `S` and read shifted by `S`'s offset. | E on app values, D on defaults |
| MariaDB | `datetime()` on `DATETIME` (Exposed DDL) | App values unchanged. Defaults keep wall-clock time in `S`; new defaults hold UTC wall-clock time. | D on defaults, to align them |
| MariaDB | `datetime()` on `TIMESTAMP` | App values were stored as instants in `S` and now read as UTC wall-clock time, shifted by `S`'s offset. Defaults now read as UTC wall-clock time. | B on app values |

The fixes, for table `t` and column `c`:

```sql
-- A (PostgreSQL)
UPDATE t SET c = (c AT TIME ZONE 'J') AT TIME ZONE 'UTC';
-- B (MariaDB)
UPDATE t SET c = CONVERT_TZ(c, '+00:00', 'S');
-- C (MariaDB)
UPDATE t SET c = CONVERT_TZ(CONVERT_TZ(c, '+00:00', 'S'), 'J', '+00:00');
-- D (MariaDB)
UPDATE t SET c = CONVERT_TZ(c, 'S', '+00:00');
-- E (MariaDB)
UPDATE t SET c = CONVERT_TZ(c, 'J', '+00:00');
```

Run them in a session with the time zone UTC, as the library's Flyway migrations do; the MariaDB
fixes on `TIMESTAMP` columns depend on it. Apply each fix once, only to rows the previous version
wrote and only to the kind of value the table names, for example with a `WHERE` clause on the row's
creation time. A column that mixes both kinds, for example written as an app value on insert and as
`CurrentTimestamp` on update, needs a way to tell the rows apart; without one, fix neither.

Named zones in `CONVERT_TZ` need the MariaDB time zone tables; offsets such as `'+02:00'` do not, but
ignore daylight saving time. **Without the tables, `CONVERT_TZ` with a named zone returns `NULL`
without an error**: the fix sets a nullable column to `NULL` and loses the value, and fails on a
`NOT NULL` column only in strict mode. The official Docker image loads the tables unless
`MARIADB_INITDB_SKIP_TZINFO` is set; check before every fix that uses named zones:

```sql
-- Must return 0. On 1, load the tables (mariadb-tzinfo-to-sql) or use offsets.
SELECT CONVERT_TZ('2030-01-01 00:00:00', 'Europe/Berlin', '+00:00') IS NULL;
```

A fix must not change other columns. On MariaDB, an `UPDATE` sets every column with
`ON UPDATE CURRENT_TIMESTAMP` to the current time unless the statement assigns it. Besides
`updated_at` of `AuditableTable` and `AuditableLongIdTable`, this affects columns created by
Exposed's `timestamp()` on MariaDB before 10.10, or later with
`explicit_defaults_for_timestamp = OFF`: there the first `TIMESTAMP` column of a table that is
declared without a default silently gets `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`.
Check the table with `SHOW CREATE TABLE t`, then assign every `ON UPDATE` column its own value,
which suppresses the update:

```sql
UPDATE t SET c = CONVERT_TZ(CONVERT_TZ(c, '+00:00', 'S'), 'J', '+00:00'), updated_at = updated_at;
```

On PostgreSQL, run the fix before you create the trigger below, or disable the trigger around it in
one transaction (a Flyway SQL migration runs in one):

```sql
ALTER TABLE t DISABLE TRIGGER t_set_updated_at;
UPDATE t SET c = (c AT TIME ZONE 'J') AT TIME ZONE 'UTC';
ALTER TABLE t ENABLE TRIGGER t_set_updated_at;
```

Before 2.4.0, the PostgreSQL `updated_at` trigger of `AuditableTable` and `AuditableLongIdTable` was
dropped right after it was created, so `updated_at` never changed on update. Tables created with 2.4.0
or later get a working trigger; for an older table run once:

```sql
CREATE TRIGGER <table>_set_updated_at BEFORE UPDATE ON <table>
    FOR EACH ROW EXECUTE FUNCTION surf_set_updated_at();
```

Transactions default to `READ COMMITTED`; before 2.4.0 they used `READ UNCOMMITTED`. On MariaDB, a
transaction then saw rows that other transactions had written but not yet committed, and might later
roll back (dirty reads). PostgreSQL never allows dirty reads, so the default makes no difference
there. To keep the old level, set it in a `SurfDatabaseConfigurer` bean:

```kotlin
@Bean
fun readUncommitted() = SurfDatabaseConfigurer {
    it.defaultR2dbcIsolationLevel = IsolationLevel.READ_UNCOMMITTED
}
```

Exposed applies the level per transaction, so MariaDB's session variable `@@tx_isolation` keeps
showing the server default and does not reflect it.

---

## License

This project is licensed under the GNU General Public License v3.0.
