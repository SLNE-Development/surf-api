package dev.slne.surf.microservice.gradle.mainclass.fixtures

/**
 * A class that has a static `main` through `@JvmStatic` and a function that is no entry point.
 */
object ObjectMain {
    @JvmStatic
    fun main(args: Array<String>) {
        println(args.size)
    }

    fun helper(args: Array<String>) {
        println(args.size)
    }
}

/**
 * Has no `main`.
 */
class NoMain {
    fun main(args: Array<String>) {
        println(args.size)
    }
}
