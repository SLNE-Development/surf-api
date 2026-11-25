package dev.slne.surf.api.spring.context

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.BeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportMessage
import org.springframework.boot.diagnostics.FailureAnalysis
import org.springframework.boot.diagnostics.FailureAnalyzer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.Environment
import org.springframework.core.io.support.SpringFactoriesLoader
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver
import org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler
import java.io.Serial

/**
 * Thrown when a context fails to start and one of Spring Boot's failure analyzers recognizes the
 * failure.
 *
 * The message holds the analysis' description and recommended action; the cause is the original
 * failure.
 */
class ContextFailureAnalyzedException internal constructor(
    displayName: String,
    analysis: FailureAnalysis,
    cause: Throwable,
) : RuntimeException(describe(displayName, analysis), cause) {

    private companion object {
        @Serial
        private const val serialVersionUID: Long = -2304176624510352874L

        fun describe(displayName: String, analysis: FailureAnalysis): String {
            val description = "Description:\n\n${analysis.description}"
            val action = analysis.action?.let { "\n\nAction:\n\n$it" }.orEmpty()

            return "The $displayName failed to start.\n\n$description$action"
        }
    }
}

/**
 * Explains why a context failed to start, with the failure analyzers Spring Boot registers in
 * `META-INF/spring.factories`.
 *
 * The analyzers are loaded from surf-api's class loader, which sees the registries of both Spring
 * Boot jars, and receive the failed context's bean factory and environment. Analyze before the
 * context closes.
 *
 * A failed refresh destroys every singleton, the condition evaluation report included. Callers
 * therefore obtain the report with [ConditionEvaluationReport.get] before the refresh and pass it
 * in; [explain] registers it again so that analyzers such as Boot's
 * `NoSuchBeanDefinitionFailureAnalyzer` read the recorded outcomes.
 */
internal object ContextFailureAnalysis {

    /**
     * The bean name under which [ConditionEvaluationReport.get] registers the report; Spring Boot
     * keeps its own constant private.
     */
    private const val CONDITION_REPORT_BEAN_NAME = "autoConfigurationReport"

    private val log = LogFactory.getLog(ContextFailureAnalysis::class.java)
    private val factories = SpringFactoriesLoader.forDefaultResourceLocation(
        ContextFailureAnalysis::class.java.classLoader,
    )

    /**
     * Returns the failure to rethrow for [failure] of [context], and logs [report] at debug level.
     *
     * Never throws: if the analysis itself fails, its exception is added to [failure] as
     * suppressed and [failure] is returned.
     *
     * @param report the condition evaluation report of [context], obtained before its refresh
     * @return a [ContextFailureAnalyzedException] wrapping [failure] if an analyzer recognizes it,
     * otherwise [failure] itself
     */
    fun explain(
        context: ConfigurableApplicationContext,
        displayName: String,
        failure: Throwable,
        report: ConditionEvaluationReport,
    ): Throwable {
        return try {
            logConditionReport(report, displayName)
            restoreConditionReport(context, report)
            val analysis = analyze(context, failure) ?: return failure

            ContextFailureAnalyzedException(displayName, analysis, failure)
        } catch (exception: Throwable) {
            failure.addSuppressed(exception)
            failure
        }
    }

    private fun restoreConditionReport(
        context: ConfigurableApplicationContext,
        report: ConditionEvaluationReport,
    ) {
        val beanFactory = context.beanFactory
        if (beanFactory.containsSingleton(CONDITION_REPORT_BEAN_NAME)) return

        beanFactory.registerSingleton(CONDITION_REPORT_BEAN_NAME, report)
    }

    private fun analyze(
        context: ConfigurableApplicationContext,
        failure: Throwable,
    ): FailureAnalysis? {
        val arguments = ArgumentResolver.of(BeanFactory::class.java, context.beanFactory)
            .and(Environment::class.java, context.environment)
        val analyzers = factories.load(
            FailureAnalyzer::class.java,
            arguments,
            FailureHandler.logging(log),
        )

        return analyzers.firstNotNullOfOrNull { analyzer -> analyzeSafely(analyzer, failure) }
    }

    private fun analyzeSafely(analyzer: FailureAnalyzer, failure: Throwable): FailureAnalysis? {
        return try {
            analyzer.analyze(failure)
        } catch (exception: Throwable) {
            log.debug("Failure analyzer ${analyzer.javaClass.name} failed", exception)
            null
        }
    }

    private fun logConditionReport(report: ConditionEvaluationReport, displayName: String) {
        if (!log.isDebugEnabled) return

        log.debug(ConditionEvaluationReportMessage(report, displayName))
    }
}
