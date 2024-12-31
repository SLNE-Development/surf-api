package dev.slne.surf.surfapi.core.server.impl.reflection

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.core.api.reflection.SurfProxy
import dev.slne.surf.surfapi.core.api.reflection.SurfReflection
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import java.lang.reflect.Proxy.newProxyInstance

@AutoService(SurfReflection::class)
class SurfReflectionImpl : SurfReflection {
    init {
        checkInstantiationByServiceLoader()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> createProxy(
        clazz: Class<T>,
        classLoader: ClassLoader
    ): T {
        require(clazz.isInterface) { "clazz must be an interface" }
        require(clazz.isAnnotationPresent(SurfProxy::class.java)) { "clazz must be annotated with @SurfProxy" }

        val surfProxy = clazz.getAnnotation(SurfProxy::class.java)
        val useQualifiedClassName = surfProxy.value == Void.TYPE || surfProxy.value == Unit::class

        require(useQualifiedClassName != surfProxy.qualifiedName.isEmpty()) { "Either clazz must have a value or qualifiedName must be specified in @SurfProxy, but not both." }

        val proxyClass: Class<*>

        if (!useQualifiedClassName) {
            proxyClass = surfProxy.value.java
        } else {
            try {
                proxyClass = Class.forName(surfProxy.qualifiedName, true, classLoader)
            } catch (_: ClassNotFoundException) {
                error("Could not find class " + surfProxy.qualifiedName)
            }
        }

        return newProxyInstance(
            classLoader,
            arrayOf(clazz),
            SurfInvocationHandler(clazz, proxyClass)
        ) as T
    }
}
