package dev.slne.surf.api.nats

/**
 * A class that [HidingClassLoader] hides, standing for a class of a soft dependency that is missing at runtime.
 */
class HiddenDependency

/**
 * A bean class whose method signature mentions [HiddenDependency].
 */
class SignatureNeedsHidden {
    fun use(dependency: HiddenDependency) = Unit
}

/**
 * A bean class whose constructor mentions [HiddenDependency] only as a type argument.
 */
class GenericArgumentNeedsHidden(val dependencies: List<HiddenDependency>)

/**
 * Defines [redefined] again from the bytes of the parent and refuses to load [HiddenDependency].
 *
 * A redefined class resolves its other classes through this loader, so reflecting on its members fails with a
 * [NoClassDefFoundError] or a [TypeNotPresentException], as it does after a soft dependency went missing.
 */
class HidingClassLoader(vararg redefined: Class<*>) : ClassLoader(HidingClassLoader::class.java.classLoader) {

    private val redefinedNames = redefined.mapTo(HashSet()) { it.name }

    override fun loadClass(name: String, resolve: Boolean): Class<*> = synchronized(getClassLoadingLock(name)) {
        when (name) {
            HiddenDependency::class.java.name -> throw ClassNotFoundException(name)
            in redefinedNames -> findLoadedClass(name) ?: define(name)
            else -> super.loadClass(name, resolve)
        }
    }

    private fun define(name: String): Class<*> {
        val bytes = checkNotNull(parent.getResourceAsStream(name.replace('.', '/') + ".class")) { "No bytes for $name" }
            .use { it.readBytes() }
        return defineClass(name, bytes, 0, bytes.size)
    }
}
