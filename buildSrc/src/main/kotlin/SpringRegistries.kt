import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer.MergeStrategy
import org.gradle.api.file.DuplicatesStrategy

private const val SPRING_DIRECTORY = "META-INF/spring"
private const val SPRING_IMPORTS_PATTERN = "$SPRING_DIRECTORY/*.imports"

/**
 * The Spring registries whose values are lists: a key may name several implementations, so the copies of a key are
 * joined with `,`.
 */
private val springListRegistries = listOf(
    "META-INF/spring.factories",
    "$SPRING_DIRECTORY/aot.factories",
)

/**
 * The Spring registries whose values are a single class or location. Several Spring jars ship their own copy; for
 * example spring-aop, spring-beans and spring-context each carry `spring.handlers` and `spring.schemas`. A joined
 * value would name a class or a file that does not exist, so a repeated key keeps its first value.
 */
private val springSingleValueRegistries = listOf(
    "META-INF/spring.handlers",
    "META-INF/spring.schemas",
)

/**
 * Merges the Spring registries that many bundled jars extend: the `.imports` files in `META-INF/spring` line by line,
 * and the properties registries per key. `spring.factories` and `spring/aot.factories` append the values of a key with
 * `,`; `spring.handlers` and `spring.schemas` keep the first value of a key.
 *
 * `META-INF/services` is merged by `core-convention` for every shadow jar.
 *
 * A transformer only sees every copy of a file if duplicates reach it instead of being dropped first, so exactly the
 * files merged here are included regardless of the duplicates strategy of the task. Every other duplicate, such as
 * `META-INF/spring.tooling`, keeps following it.
 */
fun ShadowJar.mergeSpringRegistries() {
    mergeServiceFiles {
        path = SPRING_DIRECTORY
        setIncludes(listOf(SPRING_IMPORTS_PATTERN))
    }
    mergePropertiesRegistries(springListRegistries, MergeStrategy.Append)
    mergePropertiesRegistries(springSingleValueRegistries, MergeStrategy.First)
    filesMatching(listOf(SPRING_IMPORTS_PATTERN) + springListRegistries + springSingleValueRegistries) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}

private fun ShadowJar.mergePropertiesRegistries(registries: List<String>, strategy: MergeStrategy) {
    transform<PropertiesFileTransformer> {
        paths.set(registries.map { "^${Regex.escape(it)}$" })
        mergeStrategy.set(strategy)
    }
}
