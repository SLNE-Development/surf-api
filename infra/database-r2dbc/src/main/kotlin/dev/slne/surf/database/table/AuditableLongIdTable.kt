package dev.slne.surf.database.table

import dev.slne.surf.database.columns.time.CurrentOffsetDateTime
import dev.slne.surf.database.columns.time.offsetDateTime
import org.jetbrains.exposed.v1.core.dao.id.ULongIdTable
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager

open class AuditableLongIdTable(name: String = "") : ULongIdTable(name) {
    val createdAt = offsetDateTime("created_at")
        .defaultExpression(CurrentOffsetDateTime())
    val updatedAt = offsetDateTime("updated_at")
        .defaultExpression(CurrentOffsetDateTime(true))

    /**
     * Appends the PostgreSQL trigger that keeps [updatedAt] in sync on every `UPDATE`.
     *
     * MariaDB/MySQL express this inline via `ON UPDATE CURRENT_TIMESTAMP` (see
     * [dev.slne.surf.database.columns.time.CurrentTimestampBase]), but PostgreSQL has no such
     * column clause, so a `BEFORE UPDATE` trigger is required instead. Returning the extra DDL from
     * here means Exposed's `SchemaUtils.create`/`createMissingTablesAndColumns` create the trigger
     * automatically together with the table for every subclass. On any other dialect only the base
     * statements are returned.
     */
    override fun createStatement(): List<String> {
        val statements = super.createStatement()
        if (currentDialect !is PostgreSQLDialect) return statements

        val tr = TransactionManager.current()
        val tableIdentity = tr.identity(this)
        val updatedAtColumn = tr.identity(updatedAt)
        val triggerName = tr.db.identifierManager.cutIfNecessaryAndQuote(
            "${tableName.substringAfterLast('.')}_set_updated_at"
        )

        // Shared, idempotent trigger function. Mirrors CurrentTimestampBase by storing the UTC
        // wall-clock so the value stays consistent with the UTC instants written by the app.
        // Every AuditableLongIdTable names the column `updated_at`, so the single shared function
        // is consistent across tables.
        val createFunction = """
            CREATE OR REPLACE FUNCTION $UPDATED_AT_FUNCTION() RETURNS trigger AS $$
            BEGIN
                NEW.$updatedAtColumn := (CURRENT_TIMESTAMP AT TIME ZONE 'UTC');
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
        """.trimIndent()

        // DROP + CREATE keeps it idempotent across re-runs without requiring PostgreSQL 14's
        // `CREATE OR REPLACE TRIGGER`.
        val dropTrigger = "DROP TRIGGER IF EXISTS $triggerName ON $tableIdentity"
        val createTrigger = "CREATE TRIGGER $triggerName BEFORE UPDATE ON $tableIdentity " +
            "FOR EACH ROW EXECUTE FUNCTION $UPDATED_AT_FUNCTION()"

        return statements + createFunction + dropTrigger + createTrigger
    }

    companion object {
        /** Name of the shared `plpgsql` function backing the `updated_at` trigger on PostgreSQL. */
        private const val UPDATED_AT_FUNCTION = "surf_set_updated_at"
    }
}
