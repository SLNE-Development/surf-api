package dev.slne.surf.api.reflection

/**
 * The field handle interfaces a [ReflectedField] property can return.
 *
 * @property handleType the handle interface
 * @property isStatic whether the handle binds a static field
 * @property fieldType the primitive field type the handle needs; `null` for any reference type
 */
internal enum class FieldHandleKind(
    val handleType: Class<*>,
    val isStatic: Boolean,
    val fieldType: Class<*>?,
) {
    INT(IntFieldHandle::class.java, isStatic = false, Int::class.java),
    LONG(LongFieldHandle::class.java, isStatic = false, Long::class.java),
    BOOLEAN(BooleanFieldHandle::class.java, isStatic = false, Boolean::class.java),
    OBJECT(ObjectFieldHandle::class.java, isStatic = false, fieldType = null),
    STATIC_INT(StaticIntFieldHandle::class.java, isStatic = true, Int::class.java),
    STATIC_LONG(StaticLongFieldHandle::class.java, isStatic = true, Long::class.java),
    STATIC_BOOLEAN(StaticBooleanFieldHandle::class.java, isStatic = true, Boolean::class.java),
    STATIC_OBJECT(StaticObjectFieldHandle::class.java, isStatic = true, fieldType = null),
    ;

    /**
     * Returns whether a field of [type] fits this handle.
     */
    fun accepts(type: Class<*>): Boolean {
        return if (fieldType == null) !type.isPrimitive else type == fieldType
    }

    /**
     * Describes the fields this handle accepts, for failure messages.
     */
    fun describeFieldType(): String = fieldType?.name ?: "a reference type"

    companion object {
        /**
         * Returns the kind whose handle interface is [type], or `null` if [type] is none.
         */
        fun of(type: Class<*>): FieldHandleKind? = entries.firstOrNull { it.handleType == type }
    }
}
