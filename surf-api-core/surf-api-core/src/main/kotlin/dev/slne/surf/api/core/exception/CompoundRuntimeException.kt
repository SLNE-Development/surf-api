package dev.slne.surf.api.core.exception

import java.io.PrintStream
import java.io.PrintWriter
import java.io.Serial

/**
 * Runtime exception that represents multiple nested [Throwable] instances.
 *
 * The first nested throwable is exposed as [cause]. Message, localized message,
 * string representation, and stack trace output are composed from all nested
 * throwables in their original order.
 *
 * **Thread safety:** This class is not synchronized. It stores the provided
 * [throwables] list by reference and exposes it through [exceptions]. Concurrent
 * safety requires that the provided list is not mutated while this exception is
 * being accessed. The contained [Throwable] instances must also be safe for the
 * calling context.
 *
 * @param throwables the nested throwables represented by this exception.
 */
class CompoundRuntimeException(private val throwables: List<Throwable>) : RuntimeException() {

    /**
     * Returns the first nested throwable, or `null` if no nested throwables exist.
     *
     * **Thread safety:** Safe for concurrent reads only if the backing [throwables]
     * list is not mutated concurrently.
     */
    override val cause: Throwable?
        get() = throwables.firstOrNull()

    /**
     * The nested throwables represented by this exception.
     *
     * The returned list is the original list passed to the constructor.
     *
     * **Thread safety:** Safe for concurrent reads only if the list is not mutated
     * concurrently.
     */
    val exceptions: List<Throwable> get() = throwables

    /**
     * A composite message built from all nested throwable messages.
     *
     * **Thread safety:** Safe for concurrent reads only if the backing [throwables]
     * list is not mutated concurrently and the nested throwables are safe to access.
     */
    override val message: String
        get() = processAll(Throwable::message).toString()

    /**
     * Returns a composite localized message built from all nested localized messages.
     *
     * **Thread safety:** Safe for concurrent reads only if the backing [throwables]
     * list is not mutated concurrently and the nested throwables are safe to access.
     *
     * @return the combined localized messages of all nested throwables.
     */
    override fun getLocalizedMessage(): String {
        return processAll(Throwable::getLocalizedMessage).toString()
    }

    /**
     * Returns a composite string representation of all nested throwables.
     *
     * **Thread safety:** Safe for concurrent reads only if the backing [throwables]
     * list is not mutated concurrently and the nested throwables are safe to access.
     *
     * @return the combined string representation of all nested throwables.
     */
    override fun toString(): String {
        return processAll(Throwable::toString).toString()
    }

    /**
     * Prints the stack trace of every nested throwable to [s].
     *
     * **Thread safety:** This function is not synchronized by this class. Concurrent
     * safety depends on the provided [PrintStream], the backing [throwables] list,
     * and the nested throwables.
     *
     * @param s the stream to print stack traces to.
     */
    override fun printStackTrace(s: PrintStream) {
        processAll({ t ->
            t.printStackTrace(s)
            ""
        }, s::print)
    }

    /**
     * Prints the stack trace of every nested throwable to [s].
     *
     * **Thread safety:** This function is not synchronized by this class. Concurrent
     * safety depends on the provided [PrintWriter], the backing [throwables] list,
     * and the nested throwables.
     *
     * @param s the writer to print stack traces to.
     */
    override fun printStackTrace(s: PrintWriter) {
        processAll({ t ->
            t.printStackTrace(s)
            ""
        }, s::print)
    }

    /**
     * Processes all nested throwables and returns the combined textual result.
     *
     * When only one throwable is present, no composite header or footer is added.
     * When multiple throwables are present, each processed result is prefixed with
     * its one-based index.
     *
     * If [stringProcessor] is provided, every produced text segment is passed to it
     * while the final result is being built.
     *
     * **Thread safety:** This function is not synchronized. Safe concurrent use
     * requires that the backing [throwables] list is not mutated concurrently and
     * that [exceptionProcessor] and [stringProcessor] are safe for the calling
     * context.
     *
     * @param exceptionProcessor converts each nested throwable to a text segment.
     * @param stringProcessor optional receiver for each produced text segment.
     * @return the combined text produced from all nested throwables.
     */
    private fun processAll(
        exceptionProcessor: (Throwable) -> String?,
        stringProcessor: ((String?) -> Unit)? = null
    ): CharSequence {
        if (throwables.size == 1) {
            val throwable = throwables.first()
            val s = exceptionProcessor(throwable)
            stringProcessor?.invoke(s)
            return s ?: ""
        }

        val sb = StringBuilder()
        var line: String =
            "CompositeException (" + throwables.size + " nested):\n------------------------------\n"
        stringProcessor?.invoke(line)

        sb.append(line)
        for (i in throwables.indices) {
            val exception = throwables[i]

            line = "[" + (i + 1) + "]: "
            stringProcessor?.invoke(line)
            sb.append(line)

            line = exceptionProcessor(exception) ?: "null\n"
            if (!line.endsWith("\n")) {
                line += '\n'
            }
            stringProcessor?.invoke(line)
            sb.append(line)
        }

        line = "------------------------------\n"
        stringProcessor?.invoke(line)
        sb.append(line)
        return sb
    }


    companion object {
        @Serial
        private const val serialVersionUID: Long = 2797652432986975119L

        /**
         * Throws an exception if [throwables] contains at least one throwable.
         *
         * If [throwables] contains exactly one element, that throwable is thrown
         * directly. If it contains multiple elements, they are wrapped in a
         * [CompoundRuntimeException].
         *
         * **Thread safety:** This function is not synchronized. Safe concurrent use
         * requires that [throwables] is not mutated while this function is executing.
         * The created [CompoundRuntimeException] stores the provided list by reference.
         *
         * @param throwables the throwables to inspect.
         * @throws Throwable if [throwables] contains exactly one throwable.
         * @throws CompoundRuntimeException if [throwables] contains multiple throwables.
         */
        fun throwIfNotEmpty(throwables: List<Throwable>?) {
            if (throwables.isNullOrEmpty()) return

            if (throwables.size == 1) {
                throw throwables.first()
            } else {
                throw CompoundRuntimeException(throwables)
            }
        }
    }
}