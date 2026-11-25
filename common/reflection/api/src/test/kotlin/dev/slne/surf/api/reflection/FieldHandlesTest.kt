package dev.slne.surf.api.reflection

import dev.slne.surf.api.reflection.fixture.FieldHandleTarget
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class FieldHandlesTest {

    private val proxy = reflectionProxy<FieldHandleProxy>()
    private val target = FieldHandleTarget()

    @AfterTest
    fun resetStaticFields() {
        proxy.staticCount.set(0)
        proxy.staticTotal.set(0L)
        proxy.staticFlag.set(false)
        proxy.staticLabel.set("static")
    }

    @Test
    fun `field handles are hidden classes`() {
        assertTrue(proxy.count.javaClass.isHidden)
        assertSame(proxy.count, proxy.count)
    }

    @Test
    fun `int handle runs every operation`() {
        val count = proxy.count

        count.set(target, 1)
        assertEquals(1, count.get(target))
        count.setVolatile(target, 2)
        assertEquals(2, count.getVolatile(target))
        count.setRelease(target, 3)
        assertEquals(3, count.getAcquire(target))
        assertTrue(count.compareAndSet(target, 3, 4))
        assertFalse(count.compareAndSet(target, 3, 5))
        assertEquals(4, count.getAndSet(target, 10))
        assertEquals(10, count.getAndAdd(target, 5))
        assertEquals(20, count.addAndGet(target, 5))
        assertEquals(20, count.getAndIncrement(target))
        assertEquals(22, count.incrementAndGet(target))
        assertEquals(22, count.getAndDecrement(target))
        assertEquals(20, count.decrementAndGet(target))
        assertEquals(20, count.getAndBitwiseOr(target, 0b0101))
        assertEquals(0b10101, count.getAndBitwiseAnd(target, 0b00111))
        assertEquals(0b00101, count.getAndBitwiseXor(target, 0b00011))
        assertEquals(0b00110, proxy.readCount(target))
    }

    @Test
    fun `long handle runs every operation`() {
        val total = proxy.total

        total.set(target, 1L)
        assertEquals(1L, total.get(target))
        total.setVolatile(target, 2L)
        assertEquals(2L, total.getVolatile(target))
        total.setRelease(target, 3L)
        assertEquals(3L, total.getAcquire(target))
        assertTrue(total.compareAndSet(target, 3L, Long.MAX_VALUE))
        assertFalse(total.compareAndSet(target, 3L, 5L))
        assertEquals(Long.MAX_VALUE, total.getAndSet(target, 10L))
        assertEquals(10L, total.getAndAdd(target, 5L))
        assertEquals(20L, total.addAndGet(target, 5L))
        assertEquals(20L, total.getAndIncrement(target))
        assertEquals(22L, total.incrementAndGet(target))
        assertEquals(22L, total.getAndDecrement(target))
        assertEquals(20L, total.decrementAndGet(target))
        assertEquals(20L, total.getAndBitwiseOr(target, 0b0101L))
        assertEquals(0b10101L, total.getAndBitwiseAnd(target, 0b00111L))
        assertEquals(0b00101L, total.getAndBitwiseXor(target, 0b00011L))
        assertEquals(0b00110L, total.get(target))
    }

    @Test
    fun `boolean handle runs every operation`() {
        val flag = proxy.flag

        flag.set(target, true)
        assertTrue(flag.get(target))
        flag.setVolatile(target, false)
        assertFalse(flag.getVolatile(target))
        flag.setRelease(target, true)
        assertTrue(flag.getAcquire(target))
        assertTrue(flag.compareAndSet(target, true, false))
        assertFalse(flag.compareAndSet(target, true, false))
        assertFalse(flag.getAndSet(target, true))
        assertTrue(flag.getAndBitwiseAnd(target, false))
        assertFalse(flag.getAndBitwiseOr(target, true))
        assertTrue(flag.getAndBitwiseXor(target, true))
        assertFalse(flag.get(target))
    }

    @Test
    fun `object handle runs every operation`() {
        val label = proxy.label
        val second = String(charArrayOf('b'))

        assertEquals("label", label.get(target))
        label.set(target, "a")
        assertEquals("a", label.get(target))
        label.setVolatile(target, second)
        assertSame(second, label.getVolatile(target))
        label.setRelease(target, "c")
        assertEquals("c", label.getAcquire(target))
        assertFalse(label.compareAndSet(target, String(charArrayOf('c')), "d"))
        assertTrue(label.compareAndSet(target, label.get(target), "d"))
        assertEquals("d", label.getAndSet(target, "e"))
        assertEquals("e", label.get(target))
    }

    @Test
    fun `object handle casts its value on writes`() {
        @Suppress("UNCHECKED_CAST")
        val untyped = proxy.label as ObjectFieldHandle<FieldHandleTarget, Any>

        assertFailsWith<ClassCastException> { untyped.set(target, 1) }
    }

    @Test
    fun `static int handle runs every operation`() {
        val count = proxy.staticCount

        count.set(1)
        assertEquals(1, count.get())
        count.setVolatile(2)
        assertEquals(2, count.getVolatile())
        count.setRelease(3)
        assertEquals(3, count.getAcquire())
        assertTrue(count.compareAndSet(3, 4))
        assertFalse(count.compareAndSet(3, 5))
        assertEquals(4, count.getAndSet(10))
        assertEquals(10, count.getAndAdd(5))
        assertEquals(20, count.addAndGet(5))
        assertEquals(20, count.getAndIncrement())
        assertEquals(22, count.incrementAndGet())
        assertEquals(22, count.getAndDecrement())
        assertEquals(20, count.decrementAndGet())
        assertEquals(20, count.getAndBitwiseOr(0b0101))
        assertEquals(0b10101, count.getAndBitwiseAnd(0b00111))
        assertEquals(0b00101, count.getAndBitwiseXor(0b00011))
        assertEquals(0b00110, count.get())
    }

    @Test
    fun `static long handle runs every operation`() {
        val total = proxy.staticTotal

        total.set(1L)
        assertEquals(1L, total.get())
        total.setVolatile(2L)
        assertEquals(2L, total.getVolatile())
        total.setRelease(3L)
        assertEquals(3L, total.getAcquire())
        assertTrue(total.compareAndSet(3L, 4L))
        assertFalse(total.compareAndSet(3L, 5L))
        assertEquals(4L, total.getAndSet(10L))
        assertEquals(10L, total.getAndAdd(5L))
        assertEquals(20L, total.addAndGet(5L))
        assertEquals(20L, total.getAndIncrement())
        assertEquals(22L, total.incrementAndGet())
        assertEquals(22L, total.getAndDecrement())
        assertEquals(20L, total.decrementAndGet())
        assertEquals(20L, total.getAndBitwiseOr(0b0101L))
        assertEquals(0b10101L, total.getAndBitwiseAnd(0b00111L))
        assertEquals(0b00101L, total.getAndBitwiseXor(0b00011L))
        assertEquals(0b00110L, total.get())
    }

    @Test
    fun `static boolean handle runs every operation`() {
        val flag = proxy.staticFlag

        flag.set(true)
        assertTrue(flag.get())
        flag.setVolatile(false)
        assertFalse(flag.getVolatile())
        flag.setRelease(true)
        assertTrue(flag.getAcquire())
        assertTrue(flag.compareAndSet(true, false))
        assertFalse(flag.compareAndSet(true, false))
        assertFalse(flag.getAndSet(true))
        assertTrue(flag.getAndBitwiseAnd(false))
        assertFalse(flag.getAndBitwiseOr(true))
        assertTrue(flag.getAndBitwiseXor(true))
        assertFalse(flag.get())
    }

    @Test
    fun `static object handle runs every operation`() {
        val label = proxy.staticLabel

        assertEquals("static", label.get())
        label.set("a")
        assertEquals("a", label.get())
        label.setVolatile("b")
        assertEquals("b", label.getVolatile())
        label.setRelease("c")
        assertEquals("c", label.getAcquire())
        assertTrue(label.compareAndSet(label.get(), "d"))
        assertFalse(label.compareAndSet(String(charArrayOf('d')), "x"))
        assertEquals("d", label.getAndSet("e"))
        assertEquals("e", label.get())
    }

    @Test
    fun `default field names follow the kotlin getter naming`() {
        val xValue = proxy.xValue

        xValue.set(target, 7)
        assertEquals(7, xValue.getAndIncrement(target))
        assertEquals(8, xValue.get(target))
    }

    @Test
    fun `fields of types the proxy cannot name are reached through erased calls`() {
        val hidden = proxy.hidden
        val replacement = hidden.get(target)

        assertTrue(hidden.compareAndSet(target, replacement, replacement))
        assertFailsWith<ClassCastException> { hidden.set(target, "not hidden") }
    }

    @Test
    fun `package-private target classes are reached through erased calls`() {
        val instance = reflectionProxy<TargetProxy>().create("alice")
        val name = reflectionProxy<PackagePrivateFieldHandleProxy>().name

        assertEquals("alice", name.getAndSet(instance, "bob"))
        assertEquals("bob", name.getVolatile(instance))
        assertFailsWith<ClassCastException> { name.get("not a target") }
    }

    @Test
    fun `concurrent increments are not lost`() {
        val threads = 8
        val increments = 10_000
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threads)

        try {
            repeat(threads) {
                executor.execute {
                    start.await()
                    repeat(increments) { proxy.count.getAndIncrement(target) }
                }
            }

            start.countDown()
            executor.shutdown()
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }

        assertEquals(threads * increments, proxy.count.get(target))
    }

    @Test
    fun `every broken field handle is reported at once`() {
        val message = assertFailsWith<ReflectionProxyException> {
            reflectionProxy<BrokenFieldHandleProxy>()
        }.message.orEmpty()

        assertContains(message, "getLongAsInt: field total is long; IntFieldHandle needs int")
        assertContains(message, "getIntAsObject: field count is int; ObjectFieldHandle needs a ")
        assertContains(message, "getWrongValue: value type java.lang.Integer does not fit label")
        assertContains(message, "getNarrowValue: value type java.lang.String does not fit payload")
        assertContains(message, "getWrongReceiver: receiver type java.lang.String does not fit")
        assertContains(message, "getFinalField: $FIELD_HANDLE_TARGET.fixed is final")
        assertContains(message, "getStaticFinalField: $FIELD_HANDLE_TARGET.STATIC_FIXED is final")
        assertContains(message, "getInstanceAsStatic: IntFieldHandle binds an instance field")
        assertContains(message, "getStaticAsInstance: StaticIntFieldHandle binds a static field")
        assertContains(message, "getWithAccess: access does not apply to field handles")
    }

    private companion object {
        val FIELD_HANDLE_TARGET: String = FieldHandleTarget::class.java.name
    }
}
