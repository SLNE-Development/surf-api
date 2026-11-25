package dev.slne.surf.api.gradle.internalapi

import org.objectweb.asm.*
import java.io.File
import java.util.zip.ZipFile
import kotlin.metadata.KmDeclarationContainer
import kotlin.metadata.KmProperty
import kotlin.metadata.jvm.*

/**
 * Looks up compiled classes in directories and jars and reports the annotations of the
 * declarations that bytecode references.
 *
 * Parsed classes are cached for the lifetime of the index. The index owns the opened jars and
 * closes them in [close].
 */
internal class ClassIndex(locations: Iterable<File>) : AutoCloseable {

    private val directories = locations.filter(File::isDirectory)
    private val jars = openJars(locations.filter { it.isFile && it.extension == "jar" })
    private val cache = HashMap<String, ParsedClass?>()

    /**
     * Returns the class with the internal name [internalName], or `null` if no location holds it.
     */
    fun find(internalName: String): ParsedClass? {
        if (internalName in cache) return cache[internalName]

        val parsed = readClassBytes("$internalName.class")?.let(ParsedClass::parse)
        cache[internalName] = parsed

        return parsed
    }

    /**
     * Returns [reference] with the class that declares the member as its owner.
     *
     * Bytecode names the static receiver type as a member's owner, which can be a subclass or an
     * implementing class of the declaring one. The superclass chain is searched first, then the
     * interfaces, as the JVM resolves members. A class reference, and a member no class on the
     * classpath declares, are returned unchanged.
     */
    fun declarationOf(reference: Reference): Reference {
        if (reference !is Reference.Member) return reference

        val declaring = hierarchyOf(reference.owner)
            .firstOrNull { parsed -> parsed.memberAnnotations(reference, ::find) != null }
            ?: return reference

        return reference.copy(owner = declaring.name)
    }

    /**
     * Returns the binary names of the annotations on the declaration [reference] points at and on
     * every class that encloses it.
     *
     * A member is looked up in its [Reference.owner] only; [declarationOf] resolves an inherited
     * member to its declaring class first.
     */
    fun annotationsOf(reference: Reference): Set<String> {
        val ownerAnnotations = enclosingClassAnnotations(reference.owner)
        if (reference !is Reference.Member) return ownerAnnotations

        val memberAnnotations = find(reference.owner)?.memberAnnotations(reference, ::find)

        return memberAnnotations.orEmpty() + ownerAnnotations
    }

    override fun close(): Unit = jars.forEach(ZipFile::close)

    private fun hierarchyOf(internalName: String): Sequence<ParsedClass> {
        return sequence {
            val superclasses = generateSequence(find(internalName)) { parsed ->
                parsed.superName?.let(::find)
            }.toList()
            yieldAll(superclasses)

            val visited = HashSet<String>()
            val pending = ArrayDeque(superclasses.flatMap(ParsedClass::interfaces))

            while (pending.isNotEmpty()) {
                val name = pending.removeFirst()
                if (!visited.add(name)) continue

                val parsed = find(name) ?: continue

                yield(parsed)
                pending.addAll(parsed.interfaces)
            }
        }
    }

    private fun readClassBytes(path: String): ByteArray? {
        return directories.firstNotNullOfOrNull { dir ->
            File(dir, path).takeIf(File::isFile)?.readBytes()
        } ?: jars.firstNotNullOfOrNull { jar ->
            val entry = jar.getEntry(path) ?: return@firstNotNullOfOrNull null

            jar.getInputStream(entry).use { it.readBytes() }
        }
    }

    private fun enclosingClassAnnotations(internalName: String): Set<String> {
        return generateSequence(internalName) { name ->
            name.substringBeforeLast('$', "").takeIf(String::isNotEmpty)
        }
            .flatMap { find(it)?.annotations.orEmpty() }
            .toSet()
    }
}

/**
 * Opens [files] as jars, closing the ones already opened if one fails to open.
 */
private fun openJars(files: List<File>): List<ZipFile> {
    val opened = ArrayList<ZipFile>(files.size)

    try {
        for (file in files) {
            opened += ZipFile(file)
        }
    } catch (exception: Exception) {
        for (jar in opened) {
            jar.close()
        }

        throw exception
    }

    return opened
}

/**
 * A declaration referenced from bytecode, identified by the internal name of its [owner] class.
 */
internal sealed interface Reference {
    val owner: String

    /**
     * Returns the declaration as source code names it, with dots as separators.
     */
    fun toDisplay(): String = toString().replace('/', '.')

    /**
     * A reference to the class [owner] itself.
     */
    data class Type(override val owner: String) : Reference {
        override fun toString(): String = owner
    }

    /**
     * A reference to the field or method [name] with the JVM [descriptor], declared in or
     * inherited by [owner].
     */
    data class Member(
        override val owner: String,
        val name: String,
        val descriptor: String,
        val isField: Boolean,
    ) : Reference {
        override fun toString(): String = "$owner.$name"
    }
}

/**
 * One compiled method: its annotations and the declarations its signature and body reference.
 */
internal class ParsedMethod(val annotations: Set<String>, val references: Set<Reference>)

/**
 * The parts of a compiled class the internal API checks read.
 *
 * Annotation names are binary names, such as `a.b.Outer$Inner`; class names in references and
 * [name] are internal names, such as `a/b/Outer$Inner`.
 *
 * @property interfaces the internal names of the interfaces the class implements directly.
 * @property declarationReferences the classes the class declaration and its field types
 *   reference: the superclass, the interfaces and every field type.
 * @property methods the methods, keyed by `name(descriptor)`.
 */
internal class ParsedClass(
    val name: String,
    val superName: String?,
    val interfaces: List<String>,
    val annotations: Set<String>,
    val declarationReferences: Set<Reference>,
    val methods: Map<String, ParsedMethod>,
    private val fieldAnnotations: Map<String, Set<String>>,
    private val metadata: KotlinClassMetadata?,
) {

    private fun declarationContainer(): KmDeclarationContainer? {
        return when (metadata) {
            is KotlinClassMetadata.Class -> metadata.kmClass
            is KotlinClassMetadata.FileFacade -> metadata.kmPackage
            is KotlinClassMetadata.MultiFileClassPart -> metadata.kmPackage
            else -> null
        }
    }

    /**
     * Returns the annotations of the declaration [member] resolves to in this class, or `null` if
     * this class does not declare it.
     *
     * Kotlin puts a property's annotations on a synthetic method. For a companion object's
     * property that method sits in the outer class, which [find] looks up. A synthetic
     * default-argument method (`name$default`) or constructor carries no annotations of its own;
     * it reports those of the declaration it stands for.
     */
    fun memberAnnotations(member: Reference.Member, find: (String) -> ParsedClass?): Set<String>? {
        val key = "${member.name}${member.descriptor}"
        val property = propertyFor(member, key)
        val propertyAnnotations = property?.syntheticMethodForAnnotations?.let { signature ->
            val outer = name.substringBeforeLast('$', "").takeIf(String::isNotEmpty)?.let(find)
            (methods[signature.key()] ?: outer?.methods?.get(signature.key()))?.annotations
        }.orEmpty()
        val direct = if (member.isField) {
            fieldAnnotations[member.name]
        } else {
            methodAnnotations(member)
        }
        if (direct == null && property == null) return null

        return direct.orEmpty() + propertyAnnotations
    }

    private fun methodAnnotations(member: Reference.Member): Set<String>? {
        val method = methods["${member.name}${member.descriptor}"] ?: return null

        val original = defaultArgumentOriginal(member.name, member.descriptor)

        return method.annotations + original?.annotations.orEmpty()
    }

    /**
     * Returns the method or constructor whose default-argument synthetic has [name] and
     * [descriptor], or `null` if they name no such synthetic.
     *
     * kotlinc appends one `int` mask per 32 parameters and a trailing `Object` to `name$default`,
     * which also takes the receiver of an instance method first, and a trailing
     * `DefaultConstructorMarker` to a constructor.
     */
    private fun defaultArgumentOriginal(name: String, descriptor: String): ParsedMethod? {
        val originalName = when {
            name.endsWith(DEFAULT_SUFFIX) -> name.removeSuffix(DEFAULT_SUFFIX)
            name == CONSTRUCTOR && descriptor.contains(CONSTRUCTOR_MARKER) -> CONSTRUCTOR
            else -> return null
        }

        val candidates = methods.filterKeys { key -> key.startsWith("$originalName(") }

        return candidates.entries.firstOrNull { (key, _) ->
            val originalDescriptor = key.removePrefix(originalName)
            descriptor in defaultArgumentDescriptors(originalName, originalDescriptor)
        }?.value
    }

    private fun defaultArgumentDescriptors(
        originalName: String,
        originalDescriptor: String,
    ): List<String> {
        val original = Type.getMethodType(originalDescriptor)
        val parameters = original.argumentTypes.map(Type::getDescriptor)
        val masks = List((parameters.size + 31) / 32) { "I" }
        val returnType = original.returnType.descriptor

        fun descriptorOf(parameterList: List<String>): String {
            return parameterList.joinToString("", prefix = "(", postfix = ")$returnType")
        }

        if (originalName == CONSTRUCTOR) {
            return listOf(
                descriptorOf(parameters + masks + CONSTRUCTOR_MARKER),
                descriptorOf(parameters + CONSTRUCTOR_MARKER),
            )
        }

        val receiver = "L${this.name};"

        return listOf(
            descriptorOf(parameters + masks + OBJECT),
            descriptorOf(listOf(receiver) + parameters + masks + OBJECT),
        )
    }

    private fun propertyFor(
        member: Reference.Member,
        key: String,
    ): KmProperty? {
        return declarationContainer()?.properties?.find { property ->
            if (member.isField) {
                property.fieldSignature?.name == member.name
            } else {
                property.getterSignature?.key() == key || property.setterSignature?.key() == key
            }
        }
    }

    companion object {
        private const val CONSTRUCTOR = "<init>"
        private const val DEFAULT_SUFFIX = "\$default"
        private const val CONSTRUCTOR_MARKER = "Lkotlin/jvm/internal/DefaultConstructorMarker;"
        private const val OBJECT = "Ljava/lang/Object;"

        /**
         * Parses the class file [bytes].
         */
        fun parse(bytes: ByteArray): ParsedClass {
            val collector = ClassCollector()
            ClassReader(bytes).accept(collector, ClassReader.SKIP_FRAMES)

            return collector.result()
        }
    }
}

private fun JvmMethodSignature.key() = "$name$descriptor"

private fun String.descriptorToBinaryName(): String = Type.getType(this).className

/**
 * Returns a reference to the class [type] names, or `null` for a primitive type.
 */
private fun referenceTo(type: Type): Reference.Type? {
    return when (type.sort) {
        Type.OBJECT -> Reference.Type(type.internalName)
        Type.ARRAY -> referenceTo(type.elementType)
        else -> null
    }
}

private fun String.arrayElement(): String {
    return if (startsWith("[")) Type.getType(this).elementType.internalName else this
}

private class ClassCollector : ClassVisitor(Opcodes.ASM9) {
    private lateinit var name: String
    private var superName: String? = null
    private var interfaces = emptyList<String>()
    private val annotations = HashSet<String>()
    private val declarationReferences = LinkedHashSet<Reference>()
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
        this.interfaces = interfaces?.toList().orEmpty()

        if (superName != null) {
            declarationReferences += Reference.Type(superName)
        }

        for (interfaceName in interfaces.orEmpty()) {
            declarationReferences += Reference.Type(interfaceName)
        }
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        if (descriptor == "Lkotlin/Metadata;") return metadata

        annotations += descriptor.descriptorToBinaryName()

        return null
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?,
    ): FieldVisitor {
        referenceTo(Type.getType(descriptor))?.let { declarationReferences += it }
        val fieldMarkers = fieldAnnotations.getOrPut(name, ::HashSet)

        return object : FieldVisitor(Opcodes.ASM9) {
            override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                fieldMarkers += descriptor.descriptorToBinaryName()

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
        val methodAnnotations = HashSet<String>()
        val references = LinkedHashSet<Reference>()
        val methodType = Type.getMethodType(descriptor)
        (methodType.argumentTypes + methodType.returnType).mapNotNullTo(references, ::referenceTo)
        methods["$name$descriptor"] = ParsedMethod(methodAnnotations, references)

        return MethodReferenceCollector(methodAnnotations, references)
    }

    fun result(): ParsedClass {
        return ParsedClass(
            name = name,
            superName = superName,
            interfaces = interfaces,
            annotations = annotations,
            declarationReferences = declarationReferences,
            methods = methods,
            fieldAnnotations = fieldAnnotations,
            metadata = metadata.read(),
        )
    }
}

private class MethodReferenceCollector(
    private val annotations: MutableSet<String>,
    private val references: MutableSet<Reference>,
) : MethodVisitor(Opcodes.ASM9) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        annotations += descriptor.descriptorToBinaryName()

        return null
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean,
    ) {
        references += Reference.Member(owner.arrayElement(), name, descriptor, isField = false)
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        references += Reference.Member(owner.arrayElement(), name, descriptor, isField = true)
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        references += Reference.Type(type.arrayElement())
    }

    override fun visitLdcInsn(value: Any?) {
        if (value is Type) {
            referenceTo(value)?.let { references += it }
        }
    }

    override fun visitInvokeDynamicInsn(
        name: String,
        descriptor: String,
        bootstrapMethodHandle: Handle,
        vararg bootstrapMethodArguments: Any?,
    ) {
        for (handle in bootstrapMethodArguments.filterIsInstance<Handle>()) {
            val isField = handle.tag <= Opcodes.H_PUTSTATIC
            references += Reference.Member(handle.owner, handle.name, handle.desc, isField)
        }
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
