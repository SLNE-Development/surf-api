# surf-database-r2dbc

`surf-database-r2dbc` is an R2DBC provider for **JetBrains Exposed**, enabling non-blocking database
operations in JVM applications.

It provides:

- Integration of Exposed DSL with R2DBC
- Non-blocking, reactive database operations
- Kotlin coroutines support
- Configuration-based connection pool management
- MariaDB/MySQL support via R2DBC

The library is built on top of **JetBrains Exposed**, **R2DBC**, and **Kotlin coroutines**.

---

## Concepts

### DatabaseApi

`DatabaseApi` is the central entry point for database operations.  
It manages the R2DBC connection pool and exposes the underlying
Exposed [R2dbcDatabase](https://www.jetbrains.com/help/exposed/working-with-database.html#r2dbc).

A typical application creates **exactly one** `DatabaseApi` instance and shares it across the
system.

Lifecycle:

1. Create the API from a plugin path (loads configuration)
2. Migrate the schema
3. Execute queries using Exposed
4. Shutdown on application termination

```kotlin
val databaseApi = DatabaseApi.create(pluginPath)

// Access the underlying Exposed database
databaseApi.database
```

---

## Service Pattern (recommended)

In most applications, `DatabaseApi` is wrapped inside a service that manages its lifecycle and
provides a global access point.

```kotlin
abstract class DatabaseService {

    val databaseApi = DatabaseApi.create(
        pluginPath = dataFolder.toPath(),
        poolName = "my-app-pool"
    )

    suspend fun connect() {
        migrate()
    }

    @MustBeInvokedByOverriders
    @ApiStatus.OverrideOnly
    protected open suspend fun migrate() {
        // Bring the database schema up to date with Flyway migrations
        // See the "Migrations" section below
    }

    fun disconnect() {
        databaseApi.shutdown()
    }

    companion object {
        val instance = requiredService<DatabaseService>()
        fun get() = instance
    }
}

val databaseApi get() = DatabaseService.get().databaseApi

@AutoService(DatabaseService::class)
class MyDatabaseService : DatabaseService() {
    override suspend fun migrate() {
        super.migrate()
        databaseApi.migrate("my_plugin") {
            sql("db/migrations")
        }
    }
}
```

---

## Migrations

Schema changes are applied with [Flyway](https://flywaydb.org/). Every plugin describes its own
migrations and runs them with `DatabaseApi.migrate(name) { ... }`, typically inside `connect()`.
`migrate` is a suspend function and runs on `Dispatchers.IO`.

```kotlin
suspend fun connect() {
    val result = databaseApi.migrate("my_plugin") {
        sql("db/migrations")                                  // V<n>__<desc>.sql files on the classpath
        classes(V2_BackfillItems)                             // code migrations
        classPackage("dev.slne.surf.myplugin.migrations")     // scan a package for code migrations
    }
    logger.info("Applied ${result.migrationsExecuted} migration(s), schema was at ${result.initialVersion}")
}
```

At least one of `sql`, `classes` or `classPackage` is required; each may be given more than once.
`name` must match `[a-z0-9_]{1,40}` (it is lowercased).

`result.targetVersion` is the version of the last migration executed in this run, so it is `null`
when there was nothing to do; `result.initialVersion` is `null` the first time a plugin migrates
an empty schema.

**Do not call `migrate()` inside an open transaction.** Flyway uses its own JDBC connection, and on
MariaDB the metadata locks held by your open transaction can block Flyway's DDL indefinitely.

### SQL files

SQL migrations are classpath resources named `V<version>__<description>.sql`, for example
`db/migrations/V1__create_users.sql` and `db/migrations/V2__add_items.sql`. They are looked up through
the plugin's class loader: the class loader of the lambda passed to `migrate` by default, or the one
you set with `classLoader(...)`.

A location that cannot be found fails `migrate()`, and so does a SQL file whose name does not follow
the convention (for example `V1_init.sql` with a single underscore), so a typo never silently skips
migrations.

### History table per plugin

Each `name` gets its own history table `flyway_schema_history_<name>`, so plugins that share a database
do not interfere with each other, even if they use the same versions.

### Baseline and idempotency

A database without history for `name` is baselined at version `"0"` (change it with
`baselineVersion(...)`), so a brand-new plugin still runs its `V1` on a shared, non-empty database.
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
exists, every later `migrate()` for that plugin fails. There is no repair API: fix the migration, undo
or finish what it partially did, then delete the failed row manually from
`flyway_schema_history_<name>`. This is another reason migrations must be idempotent: after fixing the
problem the migration runs again against a possibly partially changed schema.

### Generating a script (development helper)

`DatabaseApi.generateMigrationScript(...)` compares Exposed tables with the database and writes the
difference to a SQL file you can use as the next migration:

```kotlin
databaseApi.generateMigrationScript(
    ItemsTable,
    scriptDirectory = Path.of("src/main/resources/db/migrations"),
    scriptName = "V2__items"
)
```

Run it against a database that does not yet contain the change. The generated statements are **not
idempotent** (for example a plain `CREATE TABLE`); edit the file before shipping it.

### Safety

`clean` is disabled, so Flyway never drops objects from your database.

### Upgrade notes (2.4.0)

The shaded jar now merges the `META-INF/services` files of all bundled libraries (Flyway needs this
to find its database plugins). As a side effect, service registrations of the relocated libraries
that had no effect in earlier versions are now active, for example Exposed's R2DBC `TypeMapper`s and
the plugins of the R2DBC MariaDB (`AuthenticationPlugin`) and PostgreSQL (`Extension`) drivers. If
you worked around missing type mappings or driver plugins, check whether the workaround is still
needed.

---

## Configuration

### Database Config File

The `DatabaseApi.create(pluginPath)` method loads configuration from a `database.yml` file located
relative to the provided path.

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

log-level: DEBUG
```

### Runtime environment variables

`DatabaseApi.create(pluginPath)` first loads `database.yml`, then applies environment-variable
overrides before it creates the connection factory and pool:

`environment > database.yml > built-in default`

A missing variable preserves its YAML/default value. A present but malformed value fails startup and
identifies the variable. Database types and log levels are parsed case-insensitively. Pool timeout
values retain the existing millisecond semantics, including `-1` where R2DBC Pool uses it to disable
a timeout.

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
| `SURF_DATABASE_POOL_MAX_ACQUIRE_TIME_MILLIS`           | `pool.timeouts.max-acquire-time-millis`       | `10000`                                            |
| `SURF_DATABASE_POOL_MAX_CREATE_CONNECTION_TIME_MILLIS` | `pool.timeouts.max-create-connection-time-millis` | `30000`                                            |
| `SURF_DATABASE_POOL_MAX_VALIDATION_TIME_MILLIS`        | `pool.timeouts.max-validation-time-millis`    | `-1`                                               |
| `SURF_DATABASE_POOL_MAX_IDLE_TIME_MILLIS`              | `pool.timeouts.max-idle-time-millis`          | `60000`                                            |
| `SURF_DATABASE_POOL_MAX_LIFE_TIME_MILLIS`              | `pool.timeouts.max-life-time-millis`          | `1800000`                                          |

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

### Pool Name

The optional `poolName` parameter helps identify the connection pool in logs and monitoring:

```kotlin
DatabaseApi.create(
    pluginPath = dataFolder.toPath(),
    poolName = "my-plugin-pool"
)
```

If not specified, a pool name is auto-generated based on the caller class.

---

## Database Operations

`surf-database-r2dbc` is a **provider**, not a query API. All database operations are performed
using **JetBrains Exposed**.

Refer to the official Exposed documentation:

- [Exposed Documentation](https://www.jetbrains.com/help/exposed/dsl-crud-operations.html)

### Example: Simple Query

```kotlin
suspend fun findUserById(id: UUID): User? = suspendTransaction {
    UsersTable
        .select { UsersTable.id eq id }
        .singleOrNull()
        ?.toUser()
}
```

---

## Testing

For tests, you can bypass configuration loading and provide
a [ConnectionFactory](https://r2dbc.io/spec/1.0.0.RELEASE/api/io/r2dbc/spi/ConnectionFactory.html)
directly:

```kotlin
@OptIn(TestOnlyDatabaseApi::class)
val databaseApi = DatabaseApi.create(
        connectionFactory = myTestConnectionFactory
    )
```

This is useful with **Testcontainers** or in-memory databases.

### Example with Testcontainers

```kotlin
@Testcontainers
class DatabaseTest {

    companion object {
        @Container
        val mariaDb = MariaDBContainer("mariadb")
            .withDatabaseName("testdb")
    }

    lateinit var databaseApi: DatabaseApi

    @BeforeEach
    fun setup() {
        val options = ConnectionFactoryOptions.builder()
            .option(DRIVER, "mariadb")
            .option(HOST, mariaDb.host)
            .option(PORT, mariaDb.firstMappedPort)
            .option(USER, mariaDb.username)
            .option(PASSWORD, mariaDb.password)
            .option(DATABASE, mariaDb.databaseName)
            .build()

        val pool = ConnectionPool(
            ConnectionPoolConfiguration.builder()
                .connectionFactory(ConnectionFactories.get(options))
                .build()
        )

        databaseApi = DatabaseApi.create(pool)
    }

    @AfterEach
    fun teardown() {
        databaseApi.shutdown()
    }
}
```

---

## Supported Databases

The configuration-based entry point supports **MariaDB** and **PostgreSQL** through their R2DBC
drivers. Set `credentials.database-type` or `SURF_DATABASE_TYPE` to select the driver.
`credentials.schema` and `SURF_DATABASE_SCHEMA` apply only to PostgreSQL and are safely ignored for
MariaDB.

---

## Guarantees & Non-Guarantees

Guaranteed:

* Non-blocking database operations
* Connection pooling with configurable sizing and timeouts
* Integration with Exposed's DSL and type-safe queries
* Proper resource cleanup via `shutdown()`

Not guaranteed:

* Support for databases other than MariaDB and PostgreSQL without custom setup
* Automatic schema migrations without calling `migrate`
* Cross-database transactions

---

## Common Pitfalls

### 1. Not calling `shutdown()` on application termination

The connection pool must be closed explicitly:

```kotlin
Runtime.getRuntime().addShutdownHook(Thread {
    DatabaseService.get().disconnect()
})
```

Failing to do so may leave connections open and cause resource leaks.

---

### 2. Creating `DatabaseApi` after migrating

Migrating requires a database connection. Always create `DatabaseApi` **before** calling
`migrate()`:

```kotlin
// BAD
suspend fun connect() {
    migrate() // database not ready yet
    databaseApi = DatabaseApi.create(pluginPath)
}
```

```kotlin
// GOOD
suspend fun connect() {
    databaseApi = DatabaseApi.create(pluginPath)
    migrate()
}
```

Or better: initialize `databaseApi` as a class property, then call `migrate()` in
`connect()`.

---

### 3. Using blocking JDBC instead of R2DBC transactions

Exposed supports both JDBC and R2DBC. This library provides **R2DBC only**.

Always use:

```kotlin
suspendTransaction {
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

### 4. Ignoring `suspend` for `migrate()`

The `migrate()` method must be `suspend` to allow safe schema initialization:

```kotlin
// BAD
protected open fun migrate() {
    // Cannot use suspending functions here
}
```

```kotlin
// GOOD
protected open suspend fun migrate() {
    databaseApi.migrate("my_plugin") { sql("db/migrations") }
}
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

### 6. Mixing config-based and manual setup

The config-based `DatabaseApi.create(pluginPath)` overload is for production.

The `ConnectionFactory`-based overload is for tests.

Do **not** mix them:

```kotlin
// BAD
val databaseApi = DatabaseApi.create(pluginPath)
// then later try to create another with manual factory
```

Choose one approach per application lifecycle.

---

## Internal APIs

Some APIs are annotated with `@TestOnlyDatabaseApi`.

These APIs:

* are primarily intended for tests
* bypass config-based setup
* should be avoided in production code

The recommended production entry point is:

```kotlin
DatabaseApi.create(pluginPath, poolName)
```

---

## License

This project is licensed under the GNU General Public License v3.0.
