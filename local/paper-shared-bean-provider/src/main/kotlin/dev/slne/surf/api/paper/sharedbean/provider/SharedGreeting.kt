package dev.slne.surf.api.paper.sharedbean.provider

/**
 * The contract one plugin publishes for another to inject.
 *
 * It is declared here, in the providing plugin's own jar, and nowhere else. That is what makes the
 * boot test meaningful: a consumer reaches this interface only through Paper's plugin class loader
 * group — it declares this plugin with `join-classpath: true` — so the `Class` object the consumer
 * casts to is the one *this* jar's loader defined. A consumer that bundled its own copy would hold
 * an unrelated type of the same name and could not cast the instance it is handed, which is the
 * failure `ClassLoaderIsolationTest` models from synthesised bytecode.
 */
interface SharedGreeting {

    /** The greeting, which is always [GREETING]. */
    fun greet(): String

    companion object {

        /**
         * The exact text [greet] returns.
         *
         * Nothing but this plugin's own implementation can produce it, so finding it in a line the
         * *consumer* logged is proof that the consumer called across the plugin boundary rather
         * than onto something of its own.
         */
        const val GREETING = "one-shared-greeting-2f91c7"
    }
}
