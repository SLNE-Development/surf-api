package dev.slne.surf.api.reflection

/**
 * Accesses an `int` instance field of [R] with the access modes of a
 * [java.lang.invoke.VarHandle].
 *
 * Declare it as a property of a [ReflectionProxy] interface annotated with [ReflectedField]. Every
 * abstract method calls the `VarHandle` access mode of the same name, so `get` and `set` are plain
 * accesses, `getVolatile` and `setVolatile` volatile ones, `getAcquire` and `setRelease`
 * acquire/release ones, and every other operation is atomic. The default methods derive from
 * [getAndAdd].
 *
 * The atomic operations are atomic only against other `VarHandle` or atomic accesses: a plain
 * `field++` in the target class can still interleave with them.
 *
 * @param R the receiver type, the target class or one of its supertypes
 */
interface IntFieldHandle<in R> {
    fun get(receiver: R): Int
    fun set(receiver: R, value: Int)
    fun getVolatile(receiver: R): Int
    fun setVolatile(receiver: R, value: Int)
    fun getAcquire(receiver: R): Int
    fun setRelease(receiver: R, value: Int)
    fun compareAndSet(receiver: R, expected: Int, newValue: Int): Boolean
    fun getAndSet(receiver: R, value: Int): Int
    fun getAndAdd(receiver: R, delta: Int): Int
    fun getAndBitwiseOr(receiver: R, mask: Int): Int
    fun getAndBitwiseAnd(receiver: R, mask: Int): Int
    fun getAndBitwiseXor(receiver: R, mask: Int): Int

    fun addAndGet(receiver: R, delta: Int): Int = getAndAdd(receiver, delta) + delta
    fun getAndIncrement(receiver: R): Int = getAndAdd(receiver, 1)
    fun incrementAndGet(receiver: R): Int = getAndAdd(receiver, 1) + 1
    fun getAndDecrement(receiver: R): Int = getAndAdd(receiver, -1)
    fun decrementAndGet(receiver: R): Int = getAndAdd(receiver, -1) - 1
}

/**
 * Accesses a `long` instance field of [R] with the access modes of a
 * [java.lang.invoke.VarHandle].
 *
 * The operations behave as those of [IntFieldHandle].
 *
 * @param R the receiver type, the target class or one of its supertypes
 */
interface LongFieldHandle<in R> {
    fun get(receiver: R): Long
    fun set(receiver: R, value: Long)
    fun getVolatile(receiver: R): Long
    fun setVolatile(receiver: R, value: Long)
    fun getAcquire(receiver: R): Long
    fun setRelease(receiver: R, value: Long)
    fun compareAndSet(receiver: R, expected: Long, newValue: Long): Boolean
    fun getAndSet(receiver: R, value: Long): Long
    fun getAndAdd(receiver: R, delta: Long): Long
    fun getAndBitwiseOr(receiver: R, mask: Long): Long
    fun getAndBitwiseAnd(receiver: R, mask: Long): Long
    fun getAndBitwiseXor(receiver: R, mask: Long): Long

    fun addAndGet(receiver: R, delta: Long): Long = getAndAdd(receiver, delta) + delta
    fun getAndIncrement(receiver: R): Long = getAndAdd(receiver, 1)
    fun incrementAndGet(receiver: R): Long = getAndAdd(receiver, 1) + 1
    fun getAndDecrement(receiver: R): Long = getAndAdd(receiver, -1)
    fun decrementAndGet(receiver: R): Long = getAndAdd(receiver, -1) - 1
}

/**
 * Accesses a `boolean` instance field of [R] with the access modes of a
 * [java.lang.invoke.VarHandle].
 *
 * The operations behave as those of [IntFieldHandle].
 *
 * @param R the receiver type, the target class or one of its supertypes
 */
interface BooleanFieldHandle<in R> {
    fun get(receiver: R): Boolean
    fun set(receiver: R, value: Boolean)
    fun getVolatile(receiver: R): Boolean
    fun setVolatile(receiver: R, value: Boolean)
    fun getAcquire(receiver: R): Boolean
    fun setRelease(receiver: R, value: Boolean)
    fun compareAndSet(receiver: R, expected: Boolean, newValue: Boolean): Boolean
    fun getAndSet(receiver: R, value: Boolean): Boolean
    fun getAndBitwiseOr(receiver: R, mask: Boolean): Boolean
    fun getAndBitwiseAnd(receiver: R, mask: Boolean): Boolean
    fun getAndBitwiseXor(receiver: R, mask: Boolean): Boolean
}

/**
 * Accesses a reference instance field of [R] with the access modes of a
 * [java.lang.invoke.VarHandle].
 *
 * The operations behave as those of [IntFieldHandle]; [compareAndSet] compares by identity.
 *
 * @param R the receiver type, the target class or one of its supertypes
 * @param T the field's type or one of its supertypes
 */
interface ObjectFieldHandle<in R, T> {
    fun get(receiver: R): T
    fun set(receiver: R, value: T)
    fun getVolatile(receiver: R): T
    fun setVolatile(receiver: R, value: T)
    fun getAcquire(receiver: R): T
    fun setRelease(receiver: R, value: T)
    fun compareAndSet(receiver: R, expected: T, newValue: T): Boolean
    fun getAndSet(receiver: R, value: T): T
}

/**
 * Accesses a static `int` field with the access modes of a [java.lang.invoke.VarHandle].
 *
 * Declare it with [ReflectedField] and [ReflectedStatic]. The operations behave as those of
 * [IntFieldHandle].
 */
interface StaticIntFieldHandle {
    fun get(): Int
    fun set(value: Int)
    fun getVolatile(): Int
    fun setVolatile(value: Int)
    fun getAcquire(): Int
    fun setRelease(value: Int)
    fun compareAndSet(expected: Int, newValue: Int): Boolean
    fun getAndSet(value: Int): Int
    fun getAndAdd(delta: Int): Int
    fun getAndBitwiseOr(mask: Int): Int
    fun getAndBitwiseAnd(mask: Int): Int
    fun getAndBitwiseXor(mask: Int): Int

    fun addAndGet(delta: Int): Int = getAndAdd(delta) + delta
    fun getAndIncrement(): Int = getAndAdd(1)
    fun incrementAndGet(): Int = getAndAdd(1) + 1
    fun getAndDecrement(): Int = getAndAdd(-1)
    fun decrementAndGet(): Int = getAndAdd(-1) - 1
}

/**
 * Accesses a static `long` field with the access modes of a [java.lang.invoke.VarHandle].
 *
 * Declare it with [ReflectedField] and [ReflectedStatic]. The operations behave as those of
 * [IntFieldHandle].
 */
interface StaticLongFieldHandle {
    fun get(): Long
    fun set(value: Long)
    fun getVolatile(): Long
    fun setVolatile(value: Long)
    fun getAcquire(): Long
    fun setRelease(value: Long)
    fun compareAndSet(expected: Long, newValue: Long): Boolean
    fun getAndSet(value: Long): Long
    fun getAndAdd(delta: Long): Long
    fun getAndBitwiseOr(mask: Long): Long
    fun getAndBitwiseAnd(mask: Long): Long
    fun getAndBitwiseXor(mask: Long): Long

    fun addAndGet(delta: Long): Long = getAndAdd(delta) + delta
    fun getAndIncrement(): Long = getAndAdd(1)
    fun incrementAndGet(): Long = getAndAdd(1) + 1
    fun getAndDecrement(): Long = getAndAdd(-1)
    fun decrementAndGet(): Long = getAndAdd(-1) - 1
}

/**
 * Accesses a static `boolean` field with the access modes of a [java.lang.invoke.VarHandle].
 *
 * Declare it with [ReflectedField] and [ReflectedStatic]. The operations behave as those of
 * [IntFieldHandle].
 */
interface StaticBooleanFieldHandle {
    fun get(): Boolean
    fun set(value: Boolean)
    fun getVolatile(): Boolean
    fun setVolatile(value: Boolean)
    fun getAcquire(): Boolean
    fun setRelease(value: Boolean)
    fun compareAndSet(expected: Boolean, newValue: Boolean): Boolean
    fun getAndSet(value: Boolean): Boolean
    fun getAndBitwiseOr(mask: Boolean): Boolean
    fun getAndBitwiseAnd(mask: Boolean): Boolean
    fun getAndBitwiseXor(mask: Boolean): Boolean
}

/**
 * Accesses a static reference field with the access modes of a [java.lang.invoke.VarHandle].
 *
 * Declare it with [ReflectedField] and [ReflectedStatic]. The operations behave as those of
 * [IntFieldHandle]; [compareAndSet] compares by identity.
 *
 * @param T the field's type or one of its supertypes
 */
interface StaticObjectFieldHandle<T> {
    fun get(): T
    fun set(value: T)
    fun getVolatile(): T
    fun setVolatile(value: T)
    fun getAcquire(): T
    fun setRelease(value: T)
    fun compareAndSet(expected: T, newValue: T): Boolean
    fun getAndSet(value: T): T
}
