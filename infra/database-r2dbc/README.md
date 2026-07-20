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
2. Initialize tables
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
        initializeTables()
    }

    @MustBeInvokedByOverriders
    @ApiStatus.OverrideOnly
    protected open suspend fun initializeTables() {
        // Initialize database schema using Exposed
        // See: https://www.jetbrains.com/help/exposed/working-with-tables.html#dsl-create-table
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
    override suspend fun initializeTables() {
        super.initializeTables()
        // suspendTransaction {
        //     SchemaUtils.create(UsersTable, ItemsTable)
        // }
    }
}
```

---

## Configuration

### Database Config File

The `DatabaseApi.create(pluginPath)` method loads configuration from a `database.yml` file located
relative to the provided path.

Example `database.yml`:

```yaml
credentials:
  host: localhost
  port: 3306
  username: myuser
  password: mypassword
  database: mydb

pool:
  sizing:
    initialSize: 5
    minIdle: 5
    maxSize: 20

  timeouts:
    maxAcquireTimeMillis: 30000
    maxCreateConnectionTimeMillis: 10000
    maxIdleTimeMillis: 600000
    maxLifeTimeMillis: 1800000
    maxValidationTimeMillis: 5000

logLevel: DEBUG
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
| `SURF_DATABASE_LOG_LEVEL`                              | `logLevel`                                    | `DEBUG`                                            |
| `SURF_DATABASE_TYPE`                                   | `credentials.databaseType`                    | `MARIADB`                                          |
| `SURF_DATABASE_SCHEMA`                                 | `credentials.schema`                          | `public` (PostgreSQL only)                         |
| `SURF_DATABASE_HOST`                                   | `credentials.host`                            | `localhost`                                        |
| `SURF_DATABASE_PORT`                                   | `credentials.port`                            | `3306`; valid range `1..65535`                     |
| `SURF_DATABASE_NAME`                                   | `credentials.database`                        | `database`                                         |
| `SURF_DATABASE_USERNAME`                               | `credentials.username`                        | `root`                                             |
| `SURF_DATABASE_PASSWORD`                               | `credentials.password`                        | existing YAML/default value; configure as a secret |
| `SURF_DATABASE_POOL_INITIAL_SIZE`                      | `pool.sizing.initialSize`                     | `10`                                               |
| `SURF_DATABASE_POOL_MIN_IDLE`                          | `pool.sizing.minIdle`                         | `0`                                                |
| `SURF_DATABASE_POOL_MAX_SIZE`                          | `pool.sizing.maxSize`                         | `10`                                               |
| `SURF_DATABASE_POOL_MAX_ACQUIRE_TIME_MILLIS`           | `pool.timeouts.maxAcquireTimeMillis`          | `10000`                                            |
| `SURF_DATABASE_POOL_MAX_CREATE_CONNECTION_TIME_MILLIS` | `pool.timeouts.maxCreateConnectionTimeMillis` | `30000`                                            |
| `SURF_DATABASE_POOL_MAX_VALIDATION_TIME_MILLIS`        | `pool.timeouts.maxValidationTimeMillis`       | `-1`                                               |
| `SURF_DATABASE_POOL_MAX_IDLE_TIME_MILLIS`              | `pool.timeouts.maxIdleTimeMillis`             | `60000`                                            |
| `SURF_DATABASE_POOL_MAX_LIFE_TIME_MILLIS`              | `pool.timeouts.maxLifeTimeMillis`             | `1800000`                                          |

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
drivers. Set `credentials.databaseType` or `SURF_DATABASE_TYPE` to select the driver.
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
* Automatic schema migrations
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

### 2. Creating `DatabaseApi` after table initialization

Table initialization requires a database connection. Always create `DatabaseApi` **before** calling
`initializeTables()`:

```kotlin
// BAD
suspend fun connect() {
    initializeTables() // database not ready yet
    databaseApi = DatabaseApi.create(pluginPath)
}
```

```kotlin
// GOOD
suspend fun connect() {
    databaseApi = DatabaseApi.create(pluginPath)
    initializeTables()
}
```

Or better: initialize `databaseApi` as a class property, then call `initializeTables()` in
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

### 4. Ignoring `suspend` for `initializeTables()`

The `initializeTables()` method must be `suspend` to allow safe schema initialization:

```kotlin
// BAD
protected open fun initializeTables() {
    // Cannot use suspending functions here
}
```

```kotlin
// GOOD
protected open suspend fun initializeTables() {
    suspendTransaction {
        SchemaUtils.create(UsersTable)
    }
}
```

---

### 5. Not understanding connection pool limits

The connection pool has a maximum size defined in `database.yml`.

If all connections are in use, new transactions will wait up to `maxAcquireTimeMillis` before
failing.

Monitor connection usage and adjust pool sizing if needed:

```yaml
pool:
  sizing:
    maxSize: 50  # Increase if needed
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
