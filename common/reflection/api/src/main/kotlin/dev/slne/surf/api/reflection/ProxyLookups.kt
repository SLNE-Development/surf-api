package dev.slne.surf.api.reflection

import java.lang.classfile.ClassFile
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

/**
 * Provides the lookups that reflection proxies are resolved and defined with.
 *
 * A hidden class can only be defined through a lookup with full privilege access in the package
 * of the proxy interface. A private lookup from surf-api has that only when the interface is in
 * surf-api's own module; for an interface of another module, such as a plugin's unnamed module,
 * [definingLookup] defines a small anchor class next to the interface and uses the anchor's own
 * lookup.
 */
internal object ProxyLookups {

    /**
     * surf-api's own lookup, the origin of every private lookup the proxies use.
     */
    val LOOKUP: MethodHandles.Lookup = MethodHandles.lookup()

    private const val ANCHOR_SUFFIX = $$"$ReflectionProxyLookup"
    private const val ANCHOR_METHOD = "lookup"
    private val ANCHOR_METHOD_TYPE = MethodType.methodType(MethodHandles.Lookup::class.java)
    private val ANCHOR_METHOD_DESC = MethodTypeDesc.of(
        ClassDesc.of(MethodHandles.Lookup::class.java.name),
    )

    private val anchorLock = Any()

    /**
     * Returns a lookup with full privilege access in the package of [type].
     *
     * @param type the proxy interface
     * @return a lookup whose lookup class is [type] or the anchor next to it
     * @throws IllegalAccessException if surf-api cannot get a private lookup in [type]
     * @throws IllegalStateException if another class loader already defines the anchor's name
     */
    fun definingLookup(type: Class<*>): MethodHandles.Lookup {
        val lookup = MethodHandles.privateLookupIn(type, LOOKUP)
        if (lookup.hasFullPrivilegeAccess()) return lookup

        val anchor = synchronized(anchorLock) { anchorOf(type, lookup) }
        return lookup.findStatic(anchor, ANCHOR_METHOD, ANCHOR_METHOD_TYPE)
            .invoke() as MethodHandles.Lookup
    }

    private fun anchorOf(type: Class<*>, lookup: MethodHandles.Lookup): Class<*> {
        val name = type.name + ANCHOR_SUFFIX
        val anchor = try {
            lookup.defineClass(anchorClass(ClassDesc.of(name)))
        } catch (_: LinkageError) {
            lookup.findClass(name)
        }
        check(anchor.classLoader === type.classLoader) {
            "$name from ${anchor.classLoader} does not belong to ${type.classLoader}"
        }

        return anchor
    }

    private fun anchorClass(name: ClassDesc): ByteArray {
        return ClassFile.of().build(name) { classBuilder ->
            // final synthetic class <type>$ReflectionProxyLookup
            classBuilder.withFlags(ClassFile.ACC_FINAL or ClassFile.ACC_SYNTHETIC)

            // static MethodHandles.Lookup lookup() { return MethodHandles.lookup(); }
            classBuilder.withMethodBody(
                ANCHOR_METHOD,
                ANCHOR_METHOD_DESC,
                ClassFile.ACC_STATIC,
            ) { codeBuilder ->
                codeBuilder
                    .invokestatic(ConstantDescs.CD_MethodHandles, ANCHOR_METHOD, ANCHOR_METHOD_DESC)
                    .areturn()
            }
        }
    }
}
