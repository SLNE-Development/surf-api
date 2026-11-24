package dev.slne.surf.api.gradle.pluginclass

import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import javax.lang.model.SourceVersion

/**
 * The package a plugin's component scan starts at.
 *
 * The runtime scans the package of the plugin class it loads: the generated one, or the one the
 * build script names. surf-api rejects a package that equals or contains `dev.slne.surf.api`, since
 * the scan would then include surf-api itself. A plugin that shares surf-api's class loader, which
 * is the case for a Minestom plugin, may not lie inside `dev.slne.surf.api` either, because
 * surf-api's own scan covers that package.
 */
internal object BasePackage {
    private const val SURF_API_PACKAGE = "dev.slne.surf.api"

    private val packageDeclaration = Regex(
        """^[ \t]*package[ \t]+([^\s;/]+)""",
        RegexOption.MULTILINE,
    )
    private val blockComment = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
    private val lineComment = Regex("""//[^\n]*""")

    /**
     * The package for generated classes together with the warnings the build should print.
     */
    data class Resolution(val basePackage: String, val warnings: List<String>)

    /**
     * Returns the package declared by [source], or an empty string for the default package.
     */
    fun declaredPackage(source: String): String {
        val withoutComments = source.replace(blockComment, "").replace(lineComment, "")
        return packageDeclaration.find(withoutComments)?.groupValues?.get(1)?.replace("`", "")
            .orEmpty()
    }

    /**
     * Returns the distinct packages declared by [sourceFiles].
     */
    fun declaredPackages(sourceFiles: Collection<File>): Set<String> {
        return sourceFiles.mapTo(sortedSetOf()) { declaredPackage(it.readText()) }
    }

    /**
     * Returns the longest package that is a prefix, by whole segments, of every package in
     * [packages], or an empty string when they share none.
     */
    fun commonPackage(packages: Collection<String>): String {
        val segments = packages.map { if (it.isEmpty()) emptyList() else it.split('.') }
        val first = segments.firstOrNull() ?: return ""

        val shared = first.indices.takeWhile { index ->
            segments.all { it.size > index && it[index] == first[index] }
        }

        return first.take(shared.size).joinToString(".")
    }

    /**
     * Returns the package of the binary class name [className], or an empty string for the
     * default package.
     */
    fun packageOf(className: String): String = className.substringBeforeLast('.', "")

    /**
     * Returns the package generated classes go to: [explicitPackage] when it is set, otherwise the
     * common package of [sourcePackages].
     *
     * The warnings name source packages the scan of an explicit package would miss.
     *
     * @throws InvalidUserDataException if there are no sources to derive a package from, the
     * package is no valid package name, or the runtime would reject it on [platform]
     */
    fun resolve(
        sourcePackages: Collection<String>,
        explicitPackage: String?,
        platform: SurfApiPlatform,
    ): Resolution {
        val explicit = explicitPackage?.trim()?.takeIf { it.isNotEmpty() }

        if (explicit == null && sourcePackages.isEmpty()) {
            throw invalid("The plugin has no Kotlin or Java sources to derive a package from.")
        }

        val basePackage = explicit ?: commonPackage(sourcePackages)
        val problem = when {
            basePackage.isEmpty() -> "The plugin's source files share no common package."
            !SourceVersion.isName(basePackage) -> "'$basePackage' is not a valid package name."
            else -> surfApiOverlap(basePackage, platform)
        }

        if (problem != null) {
            throw invalid(problem)
        }

        val warnings = listOfNotNull(
            outsideWarning(sourcePackages, basePackage, "The base package '$basePackage'")
                .takeIf { explicit != null },
        )

        return Resolution(basePackage, warnings)
    }

    /**
     * Checks the package of [className], the class the runtime scans from, and returns the
     * warnings for source packages the scan would miss.
     *
     * @throws InvalidUserDataException if the package is empty or the runtime would reject it on
     * [platform]
     */
    fun checkScanned(
        className: String,
        sourcePackages: Collection<String>,
        platform: SurfApiPlatform,
    ): List<String> {
        val scanPackage = packageOf(className)
        val problem = when {
            scanPackage.isEmpty() -> "The class '$className' is in the default package."
            else -> surfApiOverlap(scanPackage, platform)
        }

        if (problem != null) {
            throw InvalidUserDataException("$problem Move '$className' to a package of its own.")
        }

        return listOfNotNull(
            outsideWarning(
                sourcePackages,
                scanPackage,
                "The package '$scanPackage' of the class '$className'",
            ),
        )
    }

    private fun outsideWarning(
        sourcePackages: Collection<String>,
        scanPackage: String,
        subject: String,
    ): String? {
        val outside = sourcePackages
            .filterNot { isWithin(it, scanPackage) }
            .distinct()
            .sorted()
        if (outside.isEmpty()) return null

        return "$subject does not contain the source packages ${outside.joinToString { "'$it'" }}, " +
            "so the component scan misses their components."
    }

    private fun isWithin(candidate: String, parent: String): Boolean {
        return candidate == parent || candidate.startsWith("$parent.")
    }

    private fun invalid(problem: String): InvalidUserDataException {
        return InvalidUserDataException(
            "$problem Set basePackage in the surf extension, e.g. basePackage = \"com.example.plugin\".",
        )
    }

    /**
     * Returns why the runtime refuses to scan [scanPackage] on [platform], or `null` when it
     * accepts it.
     *
     * Paper and Velocity plugins have a class loader of their own, so only a package that equals or
     * contains surf-api's is refused. A package inside surf-api is accepted there.
     */
    private fun surfApiOverlap(scanPackage: String, platform: SurfApiPlatform): String? {
        return when {
            scanPackage == SURF_API_PACKAGE ->
                "The package '$scanPackage' is surf-api's own package."

            SURF_API_PACKAGE.startsWith("$scanPackage.") ->
                "The package '$scanPackage' contains surf-api's package '$SURF_API_PACKAGE', " +
                    "so its scan would include the components of surf-api itself."

            platform == SurfApiPlatform.MINESTOM && scanPackage.startsWith("$SURF_API_PACKAGE.") ->
                "The package '$scanPackage' lies inside surf-api's package '$SURF_API_PACKAGE'. " +
                    "A Minestom plugin shares the class loader of surf-api, whose own scan covers that package."

            else -> null
        }
    }
}

/**
 * Resolves the package generated classes go to, checks the package the runtime scans, and writes
 * the base package to [outputFile].
 *
 * The base package is resolved, and the build fails on it, only when [generatesCode] is set.
 * Otherwise nothing reads it and the output is empty. Other tasks read the package through the
 * output file, so nothing evaluates the sources while the build is configured.
 */
@CacheableTask
abstract class ComputeBasePackageTask : DefaultTask() {

    /**
     * The plugin's own Kotlin and Java source files, without generated ones.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourceFiles: ConfigurableFileCollection

    /**
     * The package set by the build script. Wins over the computed one, for generated classes only.
     */
    @get:Input
    @get:Optional
    abstract val explicitBasePackage: Property<String>

    /**
     * Whether a generated class or the libraries loader needs the base package.
     */
    @get:Input
    abstract val generatesCode: Property<Boolean>

    /**
     * The class the build script names and the runtime scans from, if any.
     */
    @get:Input
    @get:Optional
    abstract val scannedClass: Property<String>

    @get:Input
    abstract val platform: Property<SurfApiPlatform>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun compute() {
        val sourcePackages = BasePackage.declaredPackages(sourceFiles.files)
        val warnings = mutableListOf<String>()

        var basePackage = ""

        if (generatesCode.get()) {
            val resolution = BasePackage.resolve(
                sourcePackages,
                explicitBasePackage.orNull,
                platform.get(),
            )
            basePackage = resolution.basePackage
            warnings += resolution.warnings
        }

        scannedClass.orNull?.let {
            warnings += BasePackage.checkScanned(
                it,
                sourcePackages,
                platform.get(),
            )
        }

        warnings.forEach(logger::warn)
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(basePackage)
        }
    }
}
