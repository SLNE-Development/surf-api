package dev.slne.surf.surfapi.gradle.generators

import com.squareup.javapoet.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import javax.lang.model.element.Modifier.*

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
}


object LibrariesLoaderGenerator {
    val PluginLoader = "io.papermc.paper.plugin.loader.PluginLoader".className()
    val PluginClasspathBuilder = "io.papermc.paper.plugin.loader.PluginClasspathBuilder".className()
    val MavenLibraryResolver =
        "io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver".className()
    val PluginLibraries = ClassName.bestGuess("PluginLibraries")
    val Map = "java.util.Map".className()
    val List = "java.util.List".className()
    val Gson = "com.google.gson.Gson".className()
    val InputStreamReader = "java.io.InputStreamReader".className()
    val StandardCharsets = "java.nio.charset.StandardCharsets".className()
    val Dependency = "org.eclipse.aether.graph.Dependency".className()
    val RemoteRepository = "org.eclipse.aether.repository.RemoteRepository".className()
    val DefaultArtifact = "org.eclipse.aether.artifact.DefaultArtifact".className()
    val NotNull = "org.jetbrains.annotations.NotNull".className()
    val String = "java.lang.String".className()
    val Stream = "java.util.stream.Stream".className()

    private fun String.className(): ClassName =
        ClassName.get(substringBeforeLast('.'), substringAfterLast('.'))

    fun Project.generateLibrariesLoaderTask(pkg: String): TaskProvider<GenerateLibrariesLoaderTask> {
        val generatedResourcesDirectory = layout.buildDirectory.dir("generated/surf-api/loader")

        return tasks.register<GenerateLibrariesLoaderTask>("generateLibrariesLoader") {
            group = "surf-api"
            outputDirectory.set(generatedResourcesDirectory)
            packageName.set(pkg)
        }
    }

    fun generate(pkg: String): JavaFile {
        val spec = TypeSpec.classBuilder("PaperLibraryLoader")
            .addJavadoc(
                """
                Auto-generated libraries loader.
                """.trimIndent()
            )
            .addModifiers(PUBLIC, FINAL)

        spec.run {
            addSuperinterface(PluginLoader)

            addMethod(MethodSpec.methodBuilder("classloader").apply {
                addModifiers(PUBLIC)
                addAnnotation(Override::class.java)
                addParameter(PluginClasspathBuilder, "classpathBuilder")
                addStatement("var resolver = new \$T()", MavenLibraryResolver)
                addStatement("var pluginLibraries = load()")
                addStatement("pluginLibraries.asDependencies().forEach(resolver::addDependency)")
                addStatement("pluginLibraries.asRepositories().forEach(resolver::addRepository)")
                addStatement("classpathBuilder.addLibrary(resolver)")
            }.build())

            addMethod(MethodSpec.methodBuilder("load").apply {
                addModifiers(PRIVATE)
                returns(PluginLibraries)

                beginControlFlow("try (var in = getClass().getResourceAsStream(\"/paper-libraries.json\"))")

                beginControlFlow("if (in == null)")
                addStatement("return new \$T(\$T.of(), \$T.of())", PluginLibraries, Map, List)
                endControlFlow()

                beginControlFlow("if (in.available() < 1)")
                addStatement("return new \$T(\$T.of(), \$T.of())", PluginLibraries, Map, List)
                endControlFlow()

                addStatement(
                    "return new \$T().fromJson(new \$T(in, \$T.UTF_8), \$T.class)",
                    Gson,
                    InputStreamReader,
                    StandardCharsets,
                    PluginLibraries
                )
                endControlFlow()
                beginControlFlow("catch (IOException e)")
                addStatement("throw new RuntimeException(e)")
                endControlFlow()
            }.build())

            addType(TypeSpec.classBuilder("PluginLibraries").apply {
                addModifiers(PRIVATE, STATIC, FINAL)

                addField(
                    FieldSpec.builder(
                        ParameterizedTypeName.get(Map, String, String),
                        "repositories",
                        PRIVATE, FINAL
                    ).build()
                )
                addField(
                    FieldSpec.builder(
                        ParameterizedTypeName.get(List, String),
                        "dependencies",
                        PRIVATE, FINAL
                    ).build()
                )

                addMethod(MethodSpec.constructorBuilder().apply {
                    addModifiers(PRIVATE)
                    addParameter(ParameterizedTypeName.get(Map, String, String), "repositories")
                    addParameter(ParameterizedTypeName.get(List, String), "dependencies")
                    addStatement("this.repositories = repositories")
                    addStatement("this.dependencies = dependencies")
                }.build())

                addMethod(MethodSpec.methodBuilder("asDependencies").apply {
                    returns(ParameterizedTypeName.get(Stream, Dependency))
                    addStatement(
                        "return dependencies.stream().map(d -> new \$T(new \$T(d), null))",
                        Dependency,
                        DefaultArtifact
                    )
                }.build())

                addMethod(MethodSpec.methodBuilder("asRepositories").apply {
                    returns(ParameterizedTypeName.get(Stream, RemoteRepository))
                    addStatement(
                        "return repositories.entrySet().stream().map(e -> new \$T.Builder(e.getKey(), \"default\", e.getValue()).build())",
                        RemoteRepository
                    )
                }.build())
            }.build())
        }

        return JavaFile.builder(pkg, spec.build())
            .indent("  ")
            .skipJavaLangImports(true)
            .build()
    }
}