package dev.slne.surf.surfapi.gradle.generators

import com.palantir.javapoet.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.lang.model.element.Modifier.*
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

const val PaperLibraryLoaderClassName = "PaperLibraryLoader"

abstract class GenerateLibrariesLoaderTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val loader = LibrariesLoaderGenerator.generate(packageName.get())
        loader.writeTo(outputDirectory.get().asFile)
    }

    fun loaderLocation(): String = "${packageName.get()}.$PaperLibraryLoaderClassName"
}


object LibrariesLoaderGenerator {
    val PluginLoader = "io.papermc.paper.plugin.loader.PluginLoader".className()
    val MavenLibraryResolver =
        "io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver".className()
    val PluginClasspathBuilder = "io.papermc.paper.plugin.loader.PluginClasspathBuilder".className()
    val Dependency = "org.eclipse.aether.graph.Dependency".className()
    val DefaultArtifact = "org.eclipse.aether.artifact.DefaultArtifact".className()
    val RemoteRepository = "org.eclipse.aether.repository.RemoteRepository".className()
    val Gson = "com.google.gson.Gson".className()
    val Stream: ClassName = ClassName.get(java.util.stream.Stream::class.java)

    fun Project.generateLibrariesLoaderTask(pkg: String): TaskProvider<GenerateLibrariesLoaderTask> {
        val generatedResourcesDirectory = layout.buildDirectory.dir("generated/surf-api/loader")

        return tasks.register<GenerateLibrariesLoaderTask>("generateLibrariesLoader") {
            group = "surf-api"
            description = "Generates the library loader class"
            outputDirectory.set(generatedResourcesDirectory)
            packageName.set(pkg)
        }
    }

    fun generate(pkg: String): JavaFile {
        val dependencyStream = ParameterizedTypeName.get(
            Stream,
            Dependency
        )
        val repositoryStream = ParameterizedTypeName.get(
            Stream,
            RemoteRepository
        )

        val asDependencies = MethodSpec.methodBuilder("asDependencies")
            .returns(dependencyStream)
            .addStatement(
                "return dependencies.stream().map(d -> new \$T(new \$T(d), null))",
                Dependency,
                DefaultArtifact
            )
            .build()

        val asRepositories = MethodSpec.methodBuilder("asRepositories")
            .returns(repositoryStream)
            .addStatement(
                "return repositories.entrySet().stream().map(e -> new \$T.Builder(e.getKey(), \"default\", e.getValue()).build())",
                RemoteRepository
            )
            .build()

        val pluginLibrariesClass = TypeSpec.recordBuilder("PluginLibraries")
            .recordConstructor(
                MethodSpec.constructorBuilder()
                    .addParameter(typeNameOf<Map<String, String>>(), "repositories")
                    .addParameter(typeNameOf<List<String>>(), "dependencies")
                    .build()
            )
            .addMethod(asDependencies)
            .addMethod(asRepositories)
            .build()

        val returnEmptyLibraries = CodeBlock.builder()
            .add(
                "return new \$N(\$T.of(), \$T.of())",
                pluginLibrariesClass,
                Map::class.java,
                List::class.java
            )
            .build()

        val loadMethod = MethodSpec.methodBuilder("loadLibraries")
            .addModifiers(PRIVATE)
            .returns(ClassName.bestGuess(pluginLibrariesClass.name()))
            .beginControlFlow("try (final var in = getClass().getResourceAsStream(\"/paper-libraries.json\"))")
            .beginControlFlow("if (in == null)")
            .addStatement(returnEmptyLibraries)
            .nextControlFlow("else if (in.available() < 1)")
            .addStatement(returnEmptyLibraries)
            .endControlFlow()
            .addStatement(
                "return new \$T().fromJson(new \$T(in, \$T.UTF_8), \$N.class)",
                Gson,
                InputStreamReader::class.java,
                StandardCharsets::class.java,
                pluginLibrariesClass
            )
            .nextControlFlow("catch (\$T e)", IOException::class.java)
            .addStatement("throw new \$T(e)", RuntimeException::class.java)
            .endControlFlow()
            .build()

        val classloaderMethod = MethodSpec.methodBuilder("classloader")
            .addAnnotation(Override::class.java)
            .addModifiers(PUBLIC)
            .addParameter(PluginClasspathBuilder, "classpathBuilder")
            .addStatement(
                "final var resolver = new \$T()",
                MavenLibraryResolver
            )
            .addStatement("final var libraries = loadLibraries()")
            .addStatement("libraries.asDependencies().forEach(resolver::addDependency)")
            .addStatement("libraries.asRepositories().forEach(resolver::addRepository)")
            .addStatement("classpathBuilder.addLibrary(resolver)")
            .build()

        val loaderClass = TypeSpec.classBuilder(PaperLibraryLoaderClassName)
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(PluginLoader)
            .addMethod(classloaderMethod)
            .addMethod(loadMethod)
            .addType(pluginLibrariesClass)
            .build()

        return JavaFile.builder(pkg, loaderClass)
            .indent("  ")
            .skipJavaLangImports(true)
            .build()
    }

    private fun String.className(): ClassName =
        ClassName.get(substringBeforeLast('.'), substringAfterLast('.'))

    private inline fun <reified T> typeNameOf(): TypeName {
        val type = typeOf<T>().javaType
        return ParameterizedTypeName.get(type)
    }
}