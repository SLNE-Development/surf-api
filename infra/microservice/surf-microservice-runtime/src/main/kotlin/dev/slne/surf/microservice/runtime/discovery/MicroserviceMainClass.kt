package dev.slne.surf.microservice.runtime.discovery

import dev.slne.surf.microservice.api.Microservice
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.net.URL

/**
 * Finds and instantiates the [Microservice] subclass that the surf-microservice processor recorded at compile time.
 *
 * The class path must contain exactly one [RESOURCE]: a UTF-8 file whose single line is the JVM binary name of the
 * class. A Kotlin `object` is used through its `INSTANCE` field, any other class through its public no-argument
 * constructor.
 */
internal object MicroserviceMainClass {
    const val RESOURCE = "META-INF/surf-microservice/main"

    /**
     * Creates the microservice named by the resource that [classLoader] finds.
     *
     * @throws MicroserviceDiscoveryException if there is not exactly one resource or its class cannot be instantiated
     */
    fun instantiate(classLoader: ClassLoader): Microservice {
        val className = readClassName(findResource(classLoader))
        val microserviceClass = loadClass(className, classLoader)
        return instanceOf(microserviceClass)
    }

    private fun findResource(classLoader: ClassLoader): URL {
        val resources = classLoader.getResources(RESOURCE).toList()
        return when (resources.size) {
            1 -> resources.single()
            0 -> throw MicroserviceDiscoveryException(
                "No $RESOURCE found on the class path. Apply the surf-microservice Gradle plugin, which runs the " +
                        "processor that writes it, and declare exactly one concrete Microservice subclass.",
            )

            else -> throw MicroserviceDiscoveryException(
                "Found ${resources.size} $RESOURCE resources, expected one: ${resources.joinToString()}",
            )
        }
    }

    private fun readClassName(resource: URL): String {
        val className = resource.readText(Charsets.UTF_8).trim()

        if (className.isEmpty()) {
            throw MicroserviceDiscoveryException("$resource does not name a class.")
        }

        return className
    }

    private fun loadClass(className: String, classLoader: ClassLoader): Class<out Microservice> {
        val loadedClass = try {
            Class.forName(className, true, classLoader)
        } catch (exception: ClassNotFoundException) {
            throw MicroserviceDiscoveryException(
                "Microservice class $className was not found.",
                exception
            )
        } catch (error: ExceptionInInitializerError) {
            throw MicroserviceDiscoveryException("Initializing $className failed.", error.cause)
        }

        if (!Microservice::class.java.isAssignableFrom(loadedClass)) {
            throw MicroserviceDiscoveryException("$className does not extend ${Microservice::class.java.name}.")
        }

        if (Modifier.isAbstract(loadedClass.modifiers)) {
            throw MicroserviceDiscoveryException("$className is abstract.")
        }

        return loadedClass.asSubclass(Microservice::class.java)
    }

    private fun instanceOf(microserviceClass: Class<out Microservice>): Microservice {
        objectInstance(microserviceClass)?.let { return it }

        val constructor = try {
            microserviceClass.getConstructor()
        } catch (exception: NoSuchMethodException) {
            throw MicroserviceDiscoveryException(
                "${microserviceClass.name} needs a public no-argument constructor or must be an object.",
                exception,
            )
        }

        return try {
            constructor.newInstance()
        } catch (exception: InvocationTargetException) {
            throw MicroserviceDiscoveryException(
                "Constructor of ${microserviceClass.name} failed.",
                exception.cause
            )
        }
    }

    private fun objectInstance(microserviceClass: Class<out Microservice>): Microservice? {
        val field = microserviceClass.fields.find { field ->
            field.name == OBJECT_INSTANCE_FIELD && Modifier.isStatic(field.modifiers) && field.type == microserviceClass
        }

        return field?.get(null) as Microservice?
    }

    private const val OBJECT_INSTANCE_FIELD = "INSTANCE"
}
