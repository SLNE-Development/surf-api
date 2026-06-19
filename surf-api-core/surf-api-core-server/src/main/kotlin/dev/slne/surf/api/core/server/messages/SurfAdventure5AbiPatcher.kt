package dev.slne.surf.api.core.server.messages

import net.bytebuddy.jar.asm.*
import net.bytebuddy.jar.asm.commons.ClassRemapper
import net.bytebuddy.jar.asm.commons.Remapper
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.getList
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

object SurfAdventure5AbiPatcher {
    private const val MARKER_ENTRY = "META-INF/surf-adventure5-abi-patched-v2"

    private const val COMPONENT_BUILDER_INTERNAL = "net/kyori/adventure/text/ComponentBuilder"
    private const val SURF_COMPONENT_BUILDER_INTERNAL = "dev/slne/surf/api/core/messages/builder/SurfComponentBuilder"
    private const val COMPONENT_EXTENSION_KT = "dev/slne/surf/api/core/messages/adventure/Component_extensionKt"


    private val defaultDependencyNames = setOf(
        "surf-paper-api",
    )

    private val internalNameMappings = mapOf(
        "net/kyori/adventure/text/BuildableComponent" to
                "net/kyori/adventure/text/Component",

        $$"net/kyori/adventure/util/Buildable$Builder" to
                "net/kyori/adventure/builder/AbstractBuilder",
    )

    private val remapper = object : Remapper(Opcodes.ASM9) {
        override fun map(internalName: String): String {
            return internalNameMappings[internalName] ?: internalName
        }
    }

    data class PatchResult(
        val jar: Path,
        val pluginName: String?,
        val status: Status,
        val changedClasses: Int = 0,
        val reason: String? = null,
    )

    enum class Status {
        PATCHED,
        SKIPPED_NO_PLUGIN_METADATA,
        SKIPPED_SELF,
        SKIPPED_NO_SURF_DEPENDENCY,
        SKIPPED_ALREADY_PATCHED,
        SKIPPED_NO_RELEVANT_BYTECODE,
    }

    data class PluginMetadata(
        val name: String?,
        val dependencies: Set<String>,
    )

    fun patchPluginsDirectory(
        pluginsDir: Path = Path.of("plugins"),
        dependencyNames: Set<String> = defaultDependencyNames,
        log: (String) -> Unit = {},
        warn: (String) -> Unit = {},
    ): List<PatchResult> {
        if (!pluginsDir.exists()) {
            log("Plugin folder does not exist: $pluginsDir")
            return emptyList()
        }

        val normalizedDependencyNames = dependencyNames.normalized()

        val results = mutableListOf<PatchResult>()

        val warningLogged = AtomicBoolean(false)

        Files.list(pluginsDir).use { stream ->
            stream
                .filter { path ->
                    path.isRegularFile() &&
                            path.fileName.toString().endsWith(".jar", ignoreCase = true)
                }
                .forEach { jar ->
                    val result = patchPluginJarIfNecessary(
                        jar = jar,
                        dependencyNames = normalizedDependencyNames,
                        log = log,
                        warn = warn,
                        loggedWarning = { warningLogged.getAndSet(true) }
                    )

                    results += result

                    when (result.status) {
                        Status.PATCHED -> log(
                            "Patched ${jar.fileName}: ${result.changedClasses} class(es) rewritten"
                        )

                        Status.SKIPPED_NO_RELEVANT_BYTECODE -> log(
                            "Skipped ${jar.fileName}: depends on surf-api, but has no old Adventure ABI refs"
                        )

                        Status.SKIPPED_ALREADY_PATCHED -> log(
                            "Skipped ${jar.fileName}: already patched"
                        )

                        else -> Unit
                    }
                }
        }

        return results
    }

    private fun patchPluginJarIfNecessary(
        jar: Path,
        dependencyNames: Set<String>,
        log: (String) -> Unit,
        warn: (String) -> Unit,
        loggedWarning: () -> Boolean
    ): PatchResult {
        val metadata = readPluginMetadata(jar)
            ?: return PatchResult(
                jar = jar,
                pluginName = null,
                status = Status.SKIPPED_NO_PLUGIN_METADATA,
            )

        val pluginName = metadata.name
        val normalizedPluginName = pluginName?.normalizePluginName()

        if (normalizedPluginName != null && normalizedPluginName in dependencyNames) {
            return PatchResult(
                jar = jar,
                pluginName = pluginName,
                status = Status.SKIPPED_SELF,
            )
        }

        val dependsOnSurfApi = metadata.dependencies
            .normalized()
            .any { it in dependencyNames }

        if (!dependsOnSurfApi) {
            return PatchResult(
                jar = jar,
                pluginName = pluginName,
                status = Status.SKIPPED_NO_SURF_DEPENDENCY,
            )
        }

        if (hasMarker(jar)) {
            return PatchResult(
                jar = jar,
                pluginName = pluginName,
                status = Status.SKIPPED_ALREADY_PATCHED,
            )
        }

        val rewriteResult = rewriteJar(jar, log, warn, loggedWarning)

        if (rewriteResult.changedClasses == 0) {
            return PatchResult(
                jar = jar,
                pluginName = pluginName,
                status = Status.SKIPPED_NO_RELEVANT_BYTECODE,
            )
        }

        return PatchResult(
            jar = jar,
            pluginName = pluginName,
            status = Status.PATCHED,
            changedClasses = rewriteResult.changedClasses,
        )
    }

    private data class RewriteResult(
        val changedClasses: Int,
    )

    private fun rewriteJar(
        jar: Path,
        log: (String) -> Unit,
        warn: (String) -> Unit,
        loggedWarning: () -> Boolean
    ): RewriteResult {
        val tmp = Files.createTempFile(
            jar.parent,
            jar.fileName.toString(),
            ".surf-abi.tmp",
        )

        var changedClasses = 0

        JarInputStream(Files.newInputStream(jar)).use { input ->
            JarOutputStream(Files.newOutputStream(tmp)).use { output ->
                val writtenEntries = hashSetOf<String>()

                while (true) {
                    val entry = input.nextJarEntry ?: break
                    val name = entry.name

                    if (!writtenEntries.add(name)) {
                        continue
                    }

                    if (name == MARKER_ENTRY) {
                        continue
                    }

                    if (isSignatureEntry(name)) {
                        continue
                    }

                    val originalBytes = input.readBytes()

                    val newBytes = if (name.endsWith(".class")) {
                        val rewritten = rewriteClass(originalBytes, name, warn, loggedWarning)

                        if (!rewritten.contentEquals(originalBytes)) {
                            changedClasses++
                        }

                        rewritten
                    } else {
                        originalBytes
                    }

                    val newEntry = JarEntry(name)
                    newEntry.time = entry.time

                    output.putNextEntry(newEntry)
                    output.write(newBytes)
                    output.closeEntry()
                }

                if (changedClasses > 0) {
                    val marker = JarEntry(MARKER_ENTRY)
                    output.putNextEntry(marker)
                    output.write(
                        "patched-at=${Instant.now()}\n".encodeToByteArray()
                    )
                    output.closeEntry()
                }
            }
        }

        if (changedClasses == 0) {
            Files.deleteIfExists(tmp)
            return RewriteResult(changedClasses = 0)
        }

        createBackup(jar, log)
        moveReplacing(tmp, jar)

        return RewriteResult(changedClasses = changedClasses)
    }

    private fun rewriteClass(
        bytes: ByteArray,
        entryName: String,
        warn: (String) -> Unit,
        loggedWarning: () -> Boolean
    ): ByteArray {
        if (!containsAnyOldAdventureReference(bytes)) {
            return bytes
        }

        if (!loggedWarning()) {
            warn("----------------- SurfAdventure5AbiPatcher -----------------")
            warn(
                "One or more plugins in the plugins directory appear to depend on surf-api and may be using the old Adventure 5 ABI. " +
                        "These plugins will be patched to update their Adventure references to the new API. " +
                        "This may not not cover all cases and you should prefer recompiling the plugins against the new API. " +
                        "If you are running this on a production server, please make a backup before proceeding. " +
                        "Stop the server within the next 30 seconds to abort patching."
            )
            warn("You will have to restart the server again after patching is complete for the changes to take effect.")
            warn("-------------------------------------------------------")
            Thread.sleep(Duration.ofSeconds(30))
        }

        try {
            val reader = ClassReader(bytes)
            val writer = ClassWriter(reader, 0)

            val visitor = LegacySurfApiCallRewriter(
                ClassRemapper(writer, remapper)
            )

            reader.accept(visitor, 0)

            return writer.toByteArray()
        } catch (throwable: Throwable) {
            throw IllegalStateException(
                "Failed to rewrite class entry $entryName",
                throwable,
            )
        }
    }

    private fun containsAnyOldAdventureReference(bytes: ByteArray): Boolean {
        return bytes.containsAscii("net/kyori/adventure/text/BuildableComponent") ||
                bytes.containsAscii($$"net/kyori/adventure/util/Buildable$Builder")
    }

    private fun readPluginMetadata(jar: Path): PluginMetadata? {
        JarFile(jar.toFile()).use { jarFile ->
            val pluginYml = jarFile.readTextEntry("plugin.yml")
            val paperPluginYml = jarFile.readTextEntry("paper-plugin.yml")

            if (pluginYml == null && paperPluginYml == null) {
                return null
            }

            var name: String? = null
            val dependencies = linkedSetOf<String>()

            if (pluginYml != null) {
                val parsed = parseBukkitPluginYml(pluginYml)
                name = parsed.name
                dependencies += parsed.dependencies
            }

            if (paperPluginYml != null) {
                val parsed = parsePaperPluginYml(paperPluginYml)
                name = name ?: parsed.name
                dependencies += parsed.dependencies
            }

            return PluginMetadata(
                name = name,
                dependencies = dependencies,
            )
        }
    }

    private fun parseBukkitPluginYml(text: String): PluginMetadata {
        val root = parseYaml(text)

        val name = root.node("name").string

        val dependencies = linkedSetOf<String>()
        dependencies += root.node("depend").getList(String::class).orEmpty()
        dependencies += root.node("softdepend").getList(String::class).orEmpty()

        return PluginMetadata(
            name = name,
            dependencies = dependencies,
        )
    }

    private fun parsePaperPluginYml(text: String): PluginMetadata {
        val root = parseYaml(text)

        val name = root.node("name").string

        val dependencies = linkedSetOf<String>()
        val dependenciesSection = root.node("dependencies")

        dependenciesSection
            .node("bootstrap")
            .childrenMap()
            .forEach { (name, _) ->
                dependencies.add(name.toString())
            }

        dependenciesSection
            .node("server")
            .childrenMap()
            .forEach { (name, _) ->
                dependencies.add(name.toString())
            }

        return PluginMetadata(
            name = name,
            dependencies = dependencies,
        )
    }

    private fun parseYaml(text: String): CommentedConfigurationNode {
        return BufferedReader(text.reader()).use { reader ->
            YamlConfigurationLoader.builder()
                .source { reader }
                .build()
                .load()
        }
    }

    private fun hasMarker(jar: Path): Boolean {
        JarFile(jar.toFile()).use { jarFile ->
            return jarFile.getJarEntry(MARKER_ENTRY) != null
        }
    }

    private fun createBackup(
        jar: Path,
        log: (String) -> Unit,
    ) {
        val backupDir = jar.parent.resolve(".surf-abi-backups")
        Files.createDirectories(backupDir)

        val backup = backupDir.resolve("${jar.fileName}.original")

        if (!Files.exists(backup)) {
            Files.copy(jar, backup)
            log("Backup created: $backup")
        }
    }

    private fun moveReplacing(
        source: Path,
        target: Path,
    ) {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: UnsupportedOperationException) {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private fun JarFile.readTextEntry(name: String): String? {
        val entry = getJarEntry(name) ?: return null

        return getInputStream(entry).use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    }

    private fun isSignatureEntry(name: String): Boolean {
        val upper = name.uppercase(Locale.ROOT)

        if (!upper.startsWith("META-INF/")) {
            return false
        }

        return upper.endsWith(".SF") ||
                upper.endsWith(".RSA") ||
                upper.endsWith(".DSA") ||
                upper.endsWith(".EC")
    }

    private fun Set<String>.normalized(): Set<String> {
        return mapTo(hashSetOf()) { it.normalizePluginName() }
    }

    private fun String.normalizePluginName(): String {
        return trim().lowercase(Locale.ROOT)
    }

    private fun ByteArray.containsAscii(value: String): Boolean {
        val needle = value.encodeToByteArray()

        if (needle.isEmpty() || needle.size > size) {
            return false
        }

        outer@ for (i in 0..size - needle.size) {
            for (j in needle.indices) {
                if (this[i + j] != needle[j]) {
                    continue@outer
                }
            }

            return true
        }

        return false
    }

    private class LegacySurfApiCallRewriter(
        next: ClassVisitor,
    ) : ClassVisitor(Opcodes.ASM9, next) {

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            val delegate = super.visitMethod(
                access,
                name,
                descriptor,
                signature,
                exceptions,
            )

            return object : MethodVisitor(Opcodes.ASM9, delegate) {

                override fun visitTypeInsn(
                    opcode: Int,
                    type: String,
                ) {
                    if (opcode == Opcodes.CHECKCAST && type == COMPONENT_BUILDER_INTERNAL) {
                        return
                    }

                    super.visitTypeInsn(opcode, type)
                }

                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    name: String,
                    descriptor: String,
                    isInterface: Boolean,
                ) {
                    if (opcode == Opcodes.INVOKESTATIC && owner == COMPONENT_EXTENSION_KT) {
                        val rewrittenDescriptor = rewriteComponentExtensionDescriptor(descriptor)

                        if (rewrittenDescriptor != null) {
                            super.visitMethodInsn(
                                opcode,
                                owner,
                                name,
                                rewrittenDescriptor,
                                isInterface,
                            )
                            return
                        }
                    }

                    super.visitMethodInsn(
                        opcode,
                        owner,
                        name,
                        descriptor,
                        isInterface,
                    )
                }
            }
        }
    }

    private fun rewriteComponentExtensionDescriptor(
        descriptor: String,
    ): String? {
        val arguments = Type.getArgumentTypes(descriptor)

        if (arguments.isEmpty()) {
            return null
        }

        if (arguments[0].sort != Type.OBJECT) {
            return null
        }

        if (arguments[0].internalName != COMPONENT_BUILDER_INTERNAL) {
            return null
        }

        val rewrittenArguments = arguments.copyOf()
        rewrittenArguments[0] = Type.getObjectType(SURF_COMPONENT_BUILDER_INTERNAL)

        val returnType = Type.getReturnType(descriptor)

        val rewrittenReturnType =
            if (returnType.sort == Type.OBJECT && returnType.internalName == COMPONENT_BUILDER_INTERNAL) {
                Type.getObjectType(SURF_COMPONENT_BUILDER_INTERNAL)
            } else {
                returnType
            }

        return Type.getMethodDescriptor(
            rewrittenReturnType,
            *rewrittenArguments,
        )
    }
}