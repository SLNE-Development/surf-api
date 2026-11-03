package dev.slne.surf.nats.gradle.testkit

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

/**
 * Base class of tests that run Gradle builds: provides a fresh [build] per test and a lazily started [stub].
 */
internal abstract class TestKitSupport {

    protected lateinit var build: TestBuild

    private var started: WeaverStub? = null

    /**
     * Weaver registry stand-in; started on first use and stopped after the test.
     */
    protected val stub: WeaverStub
        get() = started ?: WeaverStub().also { started = it }

    @BeforeEach
    fun createBuild(info: TestInfo) {
        build = TestBuild.create("${javaClass.simpleName}-${info.displayName}")
    }

    @AfterEach
    fun stopStub() {
        started?.close()
        started = null
    }

    /**
     * Returns the `weaver { url = ... }` block that points the build at [stub].
     */
    protected fun weaverStub(): String = "weaver { url = '${stub.url}' }"
}
