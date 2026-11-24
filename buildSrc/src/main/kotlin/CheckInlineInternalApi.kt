import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File
import java.util.zip.ZipFile
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.KmProperty
import kotlin.metadata.Visibility
import kotlin.metadata.isInline
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.Metadata
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.jvm.syntheticMethodForAnnotations
import kotlin.metadata.visibility

/**
 * The binary names of surf-api's API markers.
 */
object SurfApiMarkers {
    const val INTERNAL = "dev.slne.surf.api.shared.api.util.InternalSurfApi"
    const val PUBLISHED = "dev.slne.surf.api.shared.api.util.PublishedSurfApi"
}

/**
 * Fails when a public `inline` function references a declaration marked only `@InternalSurfApi`.
 *
 * The compiler copies the body of a public `inline` function into the plugin that calls it, so
 * every declaration that body references is part of the plugin's bytecode. `@InternalSurfApi`
 * declarations are left out of the ABI dumps and may change in any release; such a reference must
 * point at a `@PublishedSurfApi` declaration instead, which the dumps keep.
 */
@CacheableTask
abstract class CheckInlineInternalApi : DefaultTask() {

    /**
     * The compiled classes of the module whose `inline` functions are checked.
     */
    @get:Classpath
    abstract val classes: ConfigurableFileCollection

    /**
     * The classpath the module compiles against, where referenced declarations are looked up.
     */
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val roots = classes.files.filter(File::isDirectory)
        ClassIndex(roots + classpath.files).use { index ->
            val violations = InlineInternalApi.findViolations(roots, index)

            if (violations.isNotEmpty()) {
                throw GradleException(
                    "Public inline functions reference @InternalSurfApi declarations, which leave the ABI " +
                        "dumps. Mark each referenced declaration @PublishedSurfApi instead:\n" +
                        violations.joinToString("\n") { "  - $it" },
                )
            }
        }
    }
}

/**
 * A reference from a public `inline` function to a declaration marked only `@InternalSurfApi`.
 */
data class InlineViolation(val inlineFunction: String, val target: String) {
    override fun toString(): String = "$inlineFunction references $target"
}

/**
 * Finds [InlineViolation]s in compiled classes.
 */
object InlineInternalApi {

    /**
     * Checks every public `inline` function and inline property accessor compiled into [roots].
     */
    fun findViolations(roots: List<File>, index: ClassIndex): List<InlineViolation> {
        val classFiles = roots.flatMap { root ->
            root.walkTopDown().filter { it.isFile && it.extension == "class" }
                .map { it.relativeTo(root).invariantSeparatorsPath.removeSuffix(".class") to it }
                .toList()
        }.toMap()

        return classFiles.keys.sorted().flatMap { className ->
            val owner = index.find(className) ?: return@flatMap emptyList()

            owner.publicInlineMethods().flatMap { method ->
                val bodies = listOfNotNull(owner.methods[method]) +
                    listOfNotNull(
                        owner.methods.entries.find {
                            it.key.startsWith(
                                "${
                                    method.substringBefore(
                                        '(',
                                    )
                                }\$default(",
                            )
                        }?.value,
                    ) +
                    lambdaClassesOf(
                        className,
                        method,
                        classFiles.keys,
                    ).flatMap { index.find(it)?.methods?.values.orEmpty() }
                bodies.asSequence()
                    .flatMap { it.references }
                    .distinct()
                    .filter { index.isInternalOnly(it) }
                    .map {
                        InlineViolation(
                            "${className.toDisplay()}.${method.substringBefore('(')}",
                            it.toDisplay(),
                        )
                    }
                    .toList()
            }
        }.distinct()
    }

    /**
     * The classes the compiler generates for `crossinline` lambdas of [method]; they are copied
     * into the caller together with the function body.
     */
    private fun lambdaClassesOf(
        className: String,
        method: String,
        classNames: Set<String>,
    ): List<String> {
        val prefix = "$className\$${method.substringBefore('(')}\$"

        return classNames.filter { it.startsWith(prefix) }
    }

    private fun String.toDisplay() = replace('/', '.')
}

/**
 * Looks up compiled classes on directories and jars, and answers which declarations are marked.
 */
class ClassIndex(locations: Iterable<File>) : AutoCloseable {

    private val directories = locations.filter(File::isDirectory)
    private val jars = locations
        .filter { it.isFile && it.extension == "jar" }
        .map(::ZipFile)
    private val cache = HashMap<String, ParsedClass?>()

    /**
     * The parsed class [internalName], or `null` if no location holds it.
     */
    fun find(internalName: String): ParsedClass? {
        return cache.getOrPut(internalName) {
            val path = "$internalName.class"
            val bytes = directories.firstNotNullOfOrNull { dir ->
                File(
                    dir,
                    path,
                ).takeIf(File::isFile)?.readBytes()
            }
                ?: jars.firstNotNullOfOrNull { jar ->
                    jar.getEntry(path)?.let { entry ->
                        jar.getInputStream(
                            entry,
                        ).use { it.readBytes() }
                    }
                }
            bytes?.let(ParsedClass::parse)
        }
    }

    /**
     * Whether [reference] points at a declaration marked `@InternalSurfApi` and not
     * `@PublishedSurfApi`, directly or through a class that contains it.
     */
    fun isInternalOnly(reference: Reference): Boolean {
        val markers = when (reference) {
            is Reference.Type -> classMarkers(reference.owner)
            is Reference.Member -> memberMarkers(reference) + classMarkers(reference.owner)
        }

        return SurfApiMarkers.INTERNAL in markers && SurfApiMarkers.PUBLISHED !in markers
    }

    private fun classMarkers(internalName: String): Set<String> {
        return generateSequence(internalName) { name ->
            name.substringBeforeLast('$', "").takeIf(
                String::isNotEmpty,
            )
        }
            .flatMap { find(it)?.annotations.orEmpty() }
            .toSet()
    }

    private fun memberMarkers(member: Reference.Member): Set<String> {
        return generateSequence(find(member.owner)) { cls -> cls.superName?.let(::find) }
            .mapNotNull { cls -> cls.memberAnnotations(member, ::find) }
            .firstOrNull()
            .orEmpty()
    }

    override fun close(): Unit = jars.forEach(ZipFile::close)
}

/**
 * A declaration referenced from bytecode.
 */
sealed interface Reference {
    val owner: String

    data class Type(override val owner: String) : Reference {
        override fun toString(): String = owner
    }

    data class Member(
        override val owner: String,
        val name: String,
        val descriptor: String,
        val isField: Boolean,
    ) : Reference {
        override fun toString(): String = "$owner.$name"
    }

    fun toDisplay(): String = toString().replace('/', '.')
}

/**
 * One compiled method with the declarations its body references.
 */
class ParsedMethod(val annotations: Set<String>, val references: Set<Reference>)

/**
 * The parts of a compiled class the check needs.
 */
class ParsedClass(
    val name: String,
    val superName: String?,
    val annotations: Set<String>,
    val methods: Map<String, ParsedMethod>,
    private val fieldAnnotations: Map<String, Set<String>>,
    private val metadata: KotlinClassMetadata?,
) {

    /**
     * The `name(descriptor)` keys of the public `inline` functions and inline property accessors.
     */
    fun publicInlineMethods(): List<String> {
        val container: KmDeclarationContainer = when (metadata) {
            is KotlinClassMetadata.Class -> metadata.kmClass.takeIf { it.visibility.isPublicApi() }
                ?: return emptyList()

            is KotlinClassMetadata.FileFacade -> metadata.kmPackage
            is KotlinClassMetadata.MultiFileClassPart -> metadata.kmPackage
            else -> return emptyList()
        }
        val functions = container.functions
            .filter { it.isInline && it.visibility.isPublicApi() }
            .mapNotNull { it.signature }
        val accessors = container.properties
            .filter { it.visibility.isPublicApi() }
            .flatMap { property ->
                listOfNotNull(
                    property.getterSignature.takeIf { property.getter.isInline },
                    property.setterSignature.takeIf { property.setter?.isInline == true },
                )
            }
        return (functions + accessors)
            .map { it.key() }
            .filter { key ->
                methods[key]?.annotations?.none {
                    it == SurfApiMarkers.INTERNAL || it == SurfApiMarkers.PUBLISHED
                }
                    ?: false
            }
    }

    /**
     * The markers on the declaration [member] resolves to in this class, or `null` if it is not
     * declared here.
     *
     * A property's markers sit on a synthetic method, which the compiler puts into the outer class
     * for a companion object's property; [find] looks that class up.
     */
    fun memberAnnotations(member: Reference.Member, find: (String) -> ParsedClass?): Set<String>? {
        val key = "${member.name}${member.descriptor}"
        val property = propertyFor(member, key)
        val propertyMarkers = property?.syntheticMethodForAnnotations?.let { signature ->
            val outer = name.substringBeforeLast('$', "").takeIf(String::isNotEmpty)?.let(find)
            (methods[signature.key()] ?: outer?.methods?.get(signature.key()))?.annotations
        }.orEmpty()
        val direct = if (member.isField) fieldAnnotations[member.name] else methods[key]
            ?.annotations
        if (direct == null && property == null) return null

        return direct.orEmpty() + propertyMarkers
    }

    private fun propertyFor(
        member: Reference.Member,
        key: String,
    ): KmProperty? {
        return declarationContainer()?.properties?.find { property ->
            if (member.isField) property.fieldSignature?.name == member.name
            else property.getterSignature?.key() == key || property.setterSignature?.key() == key
        }
    }

    private fun declarationContainer(): KmDeclarationContainer? {
        return when (metadata) {
            is KotlinClassMetadata.Class -> metadata.kmClass
            is KotlinClassMetadata.FileFacade -> metadata.kmPackage
            is KotlinClassMetadata.MultiFileClassPart -> metadata.kmPackage
            else -> null
        }
    }

    companion object {

        /**
         * Parses [bytes] of one class file.
         */
        fun parse(bytes: ByteArray): ParsedClass {
            val collector = ClassCollector()
            ClassReader(bytes).accept(collector, ClassReader.SKIP_FRAMES)

            return collector.result()
        }
    }
}

private fun Visibility.isPublicApi() = this == Visibility.PUBLIC || this == Visibility.PROTECTED

private fun JvmMethodSignature.key() = "$name$descriptor"

private fun String.descriptorToMarker(): String = Type.getType(this).className

private class ClassCollector : ClassVisitor(Opcodes.ASM9) {
    private lateinit var name: String
    private var superName: String? = null
    private val annotations = HashSet<String>()
    private val methods = HashMap<String, ParsedMethod>()
    private val fieldAnnotations = HashMap<String, MutableSet<String>>()
    private val metadata = MetadataCollector()

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?,
    ) {
        this.name = name
        this.superName = superName
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        if (descriptor == "Lkotlin/Metadata;") return metadata

        annotations += descriptor.descriptorToMarker()

        return null
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?,
    ): FieldVisitor {
        val markers = fieldAnnotations.getOrPut(name, ::HashSet)

        return object : FieldVisitor(Opcodes.ASM9) {
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                markers += descriptor.descriptorToMarker()

                return null
            }
        }
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        val markers = HashSet<String>()
        val references = LinkedHashSet<Reference>()
        methods["$name$descriptor"] = ParsedMethod(markers, references)

        return object : MethodVisitor(Opcodes.ASM9) {
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                markers += descriptor.descriptorToMarker()

                return null
            }

            override fun visitMethodInsn(
                opcode: Int,
                owner: String,
                name: String,
                descriptor: String,
                isInterface: Boolean,
            ) {
                references += Reference.Member(
                    owner.arrayElement(),
                    name,
                    descriptor,
                    isField = false,
                )
            }

            override fun visitFieldInsn(
                opcode: Int,
                owner: String,
                name: String,
                descriptor: String,
            ) {
                references += Reference.Member(
                    owner.arrayElement(),
                    name,
                    descriptor,
                    isField = true,
                )
            }

            override fun visitTypeInsn(opcode: Int, type: String) {
                references += Reference.Type(type.arrayElement())
            }

            override fun visitLdcInsn(value: Any?) {
                if (value is Type && value.sort == Type.OBJECT) {
                    references += Reference.Type(
                        value.internalName,
                    )
                }
            }

            override fun visitInvokeDynamicInsn(
                name: String,
                descriptor: String,
                bootstrapMethodHandle: Handle,
                vararg bootstrapMethodArguments: Any?,
            ) {
                for (handle in bootstrapMethodArguments.filterIsInstance<Handle>()) {
                    references += Reference.Member(
                        handle.owner,
                        handle.name,
                        handle.desc,
                        isField = handle.tag <= Opcodes.H_PUTSTATIC,
                    )
                }
            }
        }
    }

    fun result(): ParsedClass {
        return ParsedClass(name, superName, annotations, methods, fieldAnnotations, metadata.read())
    }

    private fun String.arrayElement(): String {
        return if (startsWith("[")) Type.getType(this).elementType.internalName else this
    }
}

/**
 * Collects the values of a `kotlin.Metadata` annotation.
 */
private class MetadataCollector : AnnotationVisitor(Opcodes.ASM9) {
    private val values = HashMap<String, Any?>()

    override fun visit(name: String, value: Any?) {
        values[name] = value
    }

    override fun visitArray(name: String): AnnotationVisitor {
        val items = ArrayList<String>()

        return object : AnnotationVisitor(Opcodes.ASM9) {
            override fun visit(name: String?, value: Any?) {
                items += value as String
            }

            override fun visitEnd() {
                values[name] = items.toTypedArray()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun read(): KotlinClassMetadata? {
        if (values.isEmpty()) return null

        val metadata = Metadata(
            kind = values["k"] as Int?,
            metadataVersion = values["mv"] as IntArray?,
            data1 = values["d1"] as Array<String>?,
            data2 = values["d2"] as Array<String>?,
            extraString = values["xs"] as String?,
            packageName = values["pn"] as String?,
            extraInt = values["xi"] as Int?,
        )

        return runCatching { KotlinClassMetadata.readLenient(metadata) }.getOrNull()
    }
}
