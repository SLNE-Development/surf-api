package dev.slne.surf.database.logger

import dev.slne.surf.surfapi.core.api.util.getCallerClass
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.slf4j.event.Level

class ComponentSqlLogger(
    private val logger: ComponentLogger = ComponentLogger.logger(getCallerClass() ?: ComponentSqlLogger::class.java),
    private val level: Level
) : SqlLogger {

    override fun log(
        context: StatementContext,
        transaction: Transaction
    ) {
        logger.atLevel(level)
            .log("SQL: ${context.expandArgs(transaction)}")
    }
}