package dev.slne.surf.api.di.internal

import dev.slne.surf.api.di.BeanContainer
import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.condition.BeanCondition
import dev.slne.surf.api.di.condition.BeanConditionContext
import dev.slne.surf.api.di.definition.BeanConditions
import dev.slne.surf.api.di.definition.BeanDefinition
import dev.slne.surf.api.di.definition.PropertyCondition
import kotlinx.coroutines.test.runTest
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.net.URL
import dev.slne.surf.api.di.definition.BeanMetadata

class ConditionEvaluatorTest {

    @Test
    fun `a bean without conditions is accepted`() = runTest {
        assertAccepted(evaluator().evaluate(bean(BeanConditions()), nothingProvided))
    }

    @Test
    fun `a class that is not on the classpath rejects and is named in the reason`() = runTest {
        val conditions = BeanConditions(classNames = listOf(ABSENT_CLASS))

        val reason = rejection(evaluator().evaluate(bean(conditions), nothingProvided))

        assertContains(reason, ABSENT_CLASS)
        assertContains(reason, "not on the classpath")
    }

    @Test
    fun `a class that is on the classpath is accepted`() = runTest {
        val conditions = BeanConditions(classNames = listOf("java.util.UUID", "java.util.List"))

        assertAccepted(evaluator().evaluate(bean(conditions), nothingProvided))
    }

    @Test
    fun `every missing class is named, not only the first`() = runTest {
        val conditions =
            BeanConditions(classNames = listOf(ABSENT_CLASS, "java.util.UUID", "also.Absent"))

        val reason = rejection(evaluator().evaluate(bean(conditions), nothingProvided))

        assertContains(reason, ABSENT_CLASS)
        assertContains(reason, "also.Absent")
    }

    @Test
    fun `all required plugins have to be loaded`() = runTest {
        val conditions = BeanConditions(pluginIds = listOf("luckperms", "vault"))

        val reason = rejection(
            evaluator(plugins = setOf("luckperms")).evaluate(bean(conditions), nothingProvided)
        )

        assertContains(reason, "vault")
        assertFalse(
            reason.contains("luckperms"),
            "a loaded plugin must not be reported as missing",
        )
    }

    @Test
    fun `required plugins are accepted once all of them are loaded`() = runTest {
        val conditions = BeanConditions(pluginIds = listOf("luckperms", "vault"))

        assertAccepted(
            evaluator(plugins = setOf("vault", "luckperms"))
                .evaluate(bean(conditions), nothingProvided)
        )
    }

    @Test
    fun `one loaded plugin satisfies a whole group`() = runTest {
        val conditions = BeanConditions(anyPluginIds = listOf(listOf("luckperms", "permissionsex")))

        assertAccepted(
            evaluator(plugins = setOf("permissionsex"))
                .evaluate(bean(conditions), nothingProvided)
        )
    }

    @Test
    fun `a group with no loaded plugin rejects and names the group`() = runTest {
        val conditions = BeanConditions(
            anyPluginIds = listOf(listOf("luckperms", "permissionsex"), listOf("vault")),
        )

        val reason = rejection(
            evaluator(plugins = setOf("luckperms")).evaluate(bean(conditions), nothingProvided)
        )

        assertContains(reason, "vault")
    }

    @Test
    fun `a condition that does not hold rejects and names the condition`() = runTest {
        val conditions = BeanConditions(customConditions = listOf(NeverHolds::class.java.name))

        val reason = rejection(evaluator().evaluate(bean(conditions), nothingProvided))

        assertContains(reason, NeverHolds::class.java.name)
        assertContains(reason, "does not hold")
    }

    @Test
    fun `a condition that holds is accepted`() = runTest {
        val conditions = BeanConditions(customConditions = listOf(AlwaysHolds::class.java.name))

        assertAccepted(evaluator().evaluate(bean(conditions), nothingProvided))
    }

    @Test
    fun `a condition that throws rejects with the throwable in the reason`() = runTest {
        val conditions = BeanConditions(customConditions = listOf(Explodes::class.java.name))

        val reason = rejection(evaluator().evaluate(bean(conditions), nothingProvided))

        assertContains(reason, Explodes::class.java.name)
        assertContains(reason, EXPLOSION)
    }

    @Test
    fun `a class that is not a condition at all rejects instead of failing`() = runTest {
        val conditions = BeanConditions(customConditions = listOf("java.lang.String"))

        val reason = rejection(evaluator().evaluate(bean(conditions), nothingProvided))

        assertContains(reason, "BeanCondition")
    }

    @Test
    fun `a condition declared as an object sees the bean it guards and the container`() = runTest {
        val conditions = BeanConditions(customConditions = listOf(InspectsContext::class.java.name))

        assertAccepted(evaluator().evaluate(bean(conditions), nothingProvided))
    }

    @Test
    fun `environment matching ignores case`() = runTest {
        val conditions = BeanConditions(environments = listOf(listOf("production")))

        assertAccepted(
            evaluator(environments = setOf("PRODUCTION"))
                .evaluate(bean(conditions), nothingProvided)
        )
    }

    @Test
    fun `every environment group has to match on its own`() = runTest {
        val conditions = BeanConditions(
            environments = listOf(listOf("production", "staging"), listOf("paper", "folia")),
        )

        assertAccepted(
            evaluator(environments = setOf("production", "paper"))
                .evaluate(bean(conditions), nothingProvided)
        )

        val reason = rejection(
            evaluator(environments = setOf("production", "velocity"))
                .evaluate(bean(conditions), nothingProvided)
        )

        assertContains(reason, "paper")
        assertContains(reason, "velocity")
    }

    @Test
    fun `an empty environment group asks for nothing`() = runTest {
        val conditions = BeanConditions(environments = listOf(emptyList()))

        assertAccepted(evaluator().evaluate(bean(conditions), nothingProvided))
    }

    @Test
    fun `a missing bean condition rejects when another bean provides the type`() = runTest {
        val conditions = BeanConditions(missingBeans = listOf(STORAGE))

        val reason = rejection(evaluator().evaluate(bean(conditions)) { it == STORAGE })

        assertContains(reason, STORAGE)
    }

    @Test
    fun `a missing bean condition is accepted when nothing provides the type`() = runTest {
        val conditions = BeanConditions(missingBeans = listOf(STORAGE))

        assertAccepted(evaluator().evaluate(bean(conditions), nothingProvided))
    }

    @Test
    fun `an absent property counts as matching when matchIfMissing is set`() = runTest {
        val conditions = propertyOn(havingValue = "true", matchIfMissing = true)

        assertAccepted(evaluator().evaluate(bean(conditions), nothingProvided))
    }

    @Test
    fun `an absent property rejects when matchIfMissing is not set`() = runTest {
        val conditions = propertyOn(havingValue = "true", matchIfMissing = false)

        val reason = rejection(evaluator().evaluate(bean(conditions), nothingProvided))

        assertContains(reason, "feature.enabled")
        assertContains(reason, "not set")
    }

    @Test
    fun `an empty havingValue only requires the property to be set`() = runTest {
        val conditions = propertyOn(havingValue = "")

        assertAccepted(
            evaluator(properties = mapOf("feature.enabled" to "whatever"))
                .evaluate(bean(conditions), nothingProvided)
        )
    }

    @Test
    fun `a property holding a different value rejects and names both values`() = runTest {
        val conditions = propertyOn(havingValue = "true")

        val reason = rejection(
            evaluator(properties = mapOf("feature.enabled" to "false"))
                .evaluate(bean(conditions), nothingProvided)
        )

        assertContains(reason, "feature.enabled")
        assertContains(reason, "'false'")
        assertContains(reason, "'true'")
    }

    @Test
    fun `a property value matches ignoring case`() = runTest {
        val conditions = propertyOn(havingValue = "true")

        assertAccepted(
            evaluator(properties = mapOf("feature.enabled" to "TRUE"))
                .evaluate(bean(conditions), nothingProvided)
        )
    }

    @Test
    fun `a property is read from the file the condition names`() = runTest {
        val conditions = propertyOn(havingValue = "true", file = "storage.yml")

        assertAccepted(
            evaluator(properties = mapOf("storage.yml/feature.enabled" to "true"))
                .evaluate(bean(conditions), nothingProvided)
        )

        val reason = rejection(
            evaluator(properties = mapOf("feature.enabled" to "true"))
                .evaluate(bean(conditions), nothingProvided)
        )

        assertContains(reason, "storage.yml")
    }

    @Test
    fun `dependsOnBeans is ordering only and never rejects`() = runTest {
        val conditions = BeanConditions(dependsOnBeans = listOf("dev.slne.NobodyProvidesThis"))

        assertAccepted(evaluator().evaluate(bean(conditions), nothingProvided))
    }

    @Test
    fun `the first failing family decides and nothing after it is run`() = runTest {
        val conditions = BeanConditions(
            classNames = listOf(ABSENT_CLASS),
            customConditions = listOf(Explodes::class.java.name),
        )

        val reason = rejection(evaluator().evaluate(bean(conditions), nothingProvided))

        assertContains(reason, ABSENT_CLASS)
        assertFalse(
            reason.contains(EXPLOSION),
            "a condition behind a failing family must not be run",
        )
    }

    // -- fixtures --------------------------------------------------------------------------------

    /** A definition carrying nothing but [conditions], which is all the evaluator looks at. */
    private fun bean(conditions: BeanConditions) = BeanDefinition(
        className = BEAN_CLASS,
        provides = listOf(BEAN_CLASS),
        conditions = conditions,
    )

    /** One condition on `feature.enabled`, the single knob every property test turns. */
    private fun propertyOn(
        havingValue: String = "",
        matchIfMissing: Boolean = false,
        file: String = "",
    ) = BeanConditions(
        properties = listOf(
            PropertyCondition(
                key = listOf("feature", "enabled"),
                havingValue = havingValue,
                matchIfMissing = matchIfMissing,
                file = file,
            )
        )
    )

    /**
     * An evaluator over a platform that answers exactly what the test states.
     *
     * The platform is written out rather than mocked: it has five methods, and a hand-written one
     * makes the test spell out the whole world the bean is judged against.
     */
    private fun evaluator(
        plugins: Set<String> = emptySet(),
        environments: Set<String> = emptySet(),
        properties: Map<String, String> = emptyMap(),
    ) = ConditionEvaluator(
        platform = FakePlatform(plugins, environments),
        owner = OWNER,
        container = FakeContainer,
        propertyReader = FakeProperties(properties),
    )

    private fun assertAccepted(decision: Decision) {
        assertTrue(decision.accepted, "expected acceptance but got: ${decision.reason}")
        assertEquals(Decision.Accepted, decision)
    }

    private fun rejection(decision: Decision): String {
        assertFalse(decision.accepted, "expected a rejection")

        return assertNotNull(decision.reason, "a rejection has to carry a reason")
    }

    /** Answers the platform's questions from what a test put in. */
    private class FakePlatform(
        private val loadedPlugins: Set<String>,
        private val activeEnvironments: Set<String>,
    ) : ContainerPlatform {

        override fun classLoader(owner: Any): ClassLoader = FakePlatform::class.java.classLoader


        /**

         * The owner's own metadata, which for these loaders is what `getResources` returns: every

         * loader in this file overrides it for this one name so a case controls exactly which

         * files a container sees.

         */

        override fun beanSources(owner: Any): List<URL> =

            classLoader(owner).getResources(BeanMetadata.DIRECTORY).toList()

        override fun isPluginLoaded(pluginId: String) = pluginId in loadedPlugins

        override fun logger(owner: Any): PlatformLogger = SilentLogger

        override fun dataPath(owner: Any): Path = DATA_PATH

        override fun environments(): Set<String> = activeEnvironments

        // The evaluator is handed a PropertyReader of its own, so nothing reaches this one.
        override fun property(owner: Any, key: List<String>, file: String): String? = null
    }

    /** The evaluator reports through its decisions, so nothing is expected on the logger. */
    private object SilentLogger : PlatformLogger {
        override fun info(message: String) = Unit
        override fun warn(message: String) = Unit
        override fun error(message: String, throwable: Throwable?) = Unit
        override fun debug(message: String) = Unit
    }

    /**
     * A container that holds nothing.
     *
     * A condition runs before the beans it decides about exist, so the only things it can
     * legitimately read here are the container's identity and its owner.
     */
    private object FakeContainer : BeanContainer {

        override val parent: BeanContainer? get() = null

        override val owner: Any get() = OWNER

        override fun <T : Any> beanOrNull(type: KClass<T>, qualifier: String?): T? = null

        override fun <T : Any> bean(type: KClass<T>, qualifier: String?): T =
            error("no bean provides $type in this test")

        override fun <T : Any> beansOfType(type: KClass<T>): List<T> = emptyList()

        override fun <T : Any> lazyBean(type: KClass<T>, qualifier: String?): Lazy<T> =
            lazy { bean(type, qualifier) }
    }

    /**
     * Serves property values from a map, so no test needs a file on disk.
     *
     * A key is `a.b.c` for the default file and `file/a.b.c` for a named one, which is also what
     * checks that the file named by the condition reaches the reader.
     */
    private class FakeProperties(private val values: Map<String, String>) : PropertyReader {

        override fun read(dataPath: Path, key: List<String>, file: String): String? {
            assertEquals(DATA_PATH, dataPath, "the platform's data path has to reach the reader")

            val path = key.joinToString(".")

            return values[if (file.isEmpty()) path else "$file/$path"]
        }
    }

    /** Holds, so the bean it guards is created. */
    private class AlwaysHolds : BeanCondition {
        override suspend fun evaluate(context: BeanConditionContext) = true
    }

    /** Never holds, so the bean it guards is skipped. */
    private class NeverHolds : BeanCondition {
        override suspend fun evaluate(context: BeanConditionContext) = false
    }

    /** Fails instead of answering, which must cost only the bean it guards. */
    private class Explodes : BeanCondition {
        override suspend fun evaluate(context: BeanConditionContext): Boolean = error(EXPLOSION)
    }

    /**
     * Holds only if the context describes the bean the condition was asked about.
     *
     * Written as an `object` on purpose: a stateless condition is natural to declare this way, and
     * it is the one shape whose constructor is private.
     */
    private object InspectsContext : BeanCondition {
        override suspend fun evaluate(context: BeanConditionContext) =
            context.beanClassName == BEAN_CLASS &&
                    context.owner === OWNER &&
                    context.container === FakeContainer
    }
}

/** A class name nothing on the test classpath answers to. */
private const val ABSENT_CLASS = "dev.slne.surf.api.di.NotOnTheClasspath"

/** The bean every test judges. */
private const val BEAN_CLASS = "dev.slne.surf.api.di.internal.TestBean"

/** The type the `missingBeans` tests fight over. */
private const val STORAGE = "dev.slne.surf.api.di.internal.Storage"

/** What a failing condition throws, looked for in the rejection reason. */
private const val EXPLOSION = "the file system is on fire"

/** The application the container belongs to; identity is all a condition checks. */
private val OWNER = Any()

/** Where the fake platform keeps configuration, handed to the property reader unchanged. */
private val DATA_PATH: Path = Path.of("build", "test-data")

/** Nothing is provided, which is the uninteresting case for `missingBeans`. */
private val nothingProvided: (String) -> Boolean = { false }
