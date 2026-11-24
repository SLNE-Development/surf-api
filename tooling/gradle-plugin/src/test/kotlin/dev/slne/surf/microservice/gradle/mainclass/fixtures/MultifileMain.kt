@file:JvmName("MultifileMain")
@file:JvmMultifileClass

package dev.slne.surf.microservice.gradle.mainclass.fixtures.multifile

/**
 * Compiles to the facade `MultifileMain` and to the synthetic part class
 * `MultifileMain__MultifileMainKt`, which both declare a `public static void main(String[])`.
 */
fun main(args: Array<String>) {
    println(args.size)
}
