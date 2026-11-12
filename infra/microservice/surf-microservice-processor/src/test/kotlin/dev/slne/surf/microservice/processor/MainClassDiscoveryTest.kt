package dev.slne.surf.microservice.processor

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import dev.slne.surf.microservice.processor.support.ProcessorRun
import dev.slne.surf.microservice.processor.support.TestSources
import dev.slne.surf.microservice.processor.support.TestSources.microserviceApi
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCompilerApi::class)
class MainClassDiscoveryTest {

    @Test
    fun `writes the binary name of the only microservice class`() {
        val run = compile(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            class PunishMicroservice : Microservice()
            """
        )

        assertMainClass("com.example.PunishMicroservice", run)
    }

    @Test
    fun `accepts an object`() {
        val run = compile(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            object PunishMicroservice : Microservice()
            """
        )

        assertMainClass("com.example.PunishMicroservice", run)
    }

    @Test
    fun `uses the binary name of a nested class`() {
        val run = compile(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            class Outer {
                object Service : Microservice()
            }
            """
        )

        assertMainClass("com.example.Outer\$Service", run)
    }

    @Test
    fun `supports the default package`() {
        val run = compile(
            """
            import dev.slne.surf.microservice.api.Microservice
            class PunishMicroservice : Microservice()
            """
        )

        assertMainClass("PunishMicroservice", run)
    }

    @Test
    fun `skips abstract and sealed intermediate classes`() {
        val run = compile(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            abstract class BaseMicroservice : Microservice()
            sealed class SealedMicroservice : BaseMicroservice()
            interface Marker
            class PunishMicroservice : SealedMicroservice(), Marker
            """
        )

        assertMainClass("com.example.PunishMicroservice", run)
    }

    @Test
    fun `follows type aliases in the supertype hierarchy`() {
        val run = compile(
            """
            package com.example
            typealias Base = dev.slne.surf.microservice.api.Microservice
            class PunishMicroservice : Base()
            """
        )

        assertMainClass("com.example.PunishMicroservice", run)
    }

    @Test
    fun `accepts a primary constructor whose parameters all have defaults`() {
        val run = compile(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            class PunishMicroservice(val port: Int = 8080) : Microservice()
            """
        )

        assertMainClass("com.example.PunishMicroservice", run)
    }

    @Test
    fun `accepts a Java subclass`() {
        val javaSource = SourceFile.java(
            "PunishMicroservice.java",
            """
            package com.example;
            public class PunishMicroservice extends dev.slne.surf.microservice.api.Microservice {}
            """
        )

        val run = ProcessorRun.compile(microserviceApi, javaSource)

        assertMainClass("com.example.PunishMicroservice", run)
    }

    @Test
    fun `accepts static nested Java classes`() {
        val javaSource = SourceFile.java(
            "Holder.java",
            """
            package com.example;
            public interface Holder {
                class PunishMicroservice extends dev.slne.surf.microservice.api.Microservice {}
            }
            """
        )

        val run = ProcessorRun.compile(microserviceApi, javaSource)

        assertMainClass("com.example.Holder\$PunishMicroservice", run)
    }

    @Test
    fun `fails without a microservice class`() {
        val run = compile(
            """
            package com.example
            class Unrelated
            """
        )

        assertEquals(ExitCode.COMPILATION_ERROR, run.exitCode)
        assertContains(
            run.messages,
            "No concrete subclass of dev.slne.surf.microservice.api.Microservice found in this module"
        )
        assertNull(run.resource(MicroserviceResources.MAIN_CLASS_PATH))
    }

    @Test
    fun `fails with two microservice classes and names both`() {
        val run = compile(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            open class FirstMicroservice : Microservice()
            class SecondMicroservice : FirstMicroservice()
            """
        )

        assertEquals(ExitCode.COMPILATION_ERROR, run.exitCode)
        assertContains(run.messages, "Found 2 concrete subclasses")
        assertContains(run.messages, "com.example.FirstMicroservice, com.example.SecondMicroservice")
    }

    @Test
    fun `accepts an internal class`() {
        val run = compile(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            internal class PunishMicroservice : Microservice()
            """
        )

        assertMainClass("com.example.PunishMicroservice", run)
    }

    @Test
    fun `fails for a private class`() {
        assertNotInstantiable(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            private class PunishMicroservice : Microservice()
            """,
            "must be public or internal"
        )
    }

    @Test
    fun `fails for a public class nested in a private class`() {
        assertNotInstantiable(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            private class Outer {
                class PunishMicroservice : Microservice()
            }
            """,
            "must be public or internal"
        )
    }

    @Test
    fun `fails for a class without a no-argument constructor`() {
        assertNotInstantiable(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            class PunishMicroservice(val port: Int) : Microservice()
            """,
            "public constructor without parameters"
        )
    }

    @Test
    fun `fails for a companion object`() {
        assertNotInstantiable(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            class Holder {
                companion object : Microservice()
            }
            """,
            "companion object"
        )
    }

    @Test
    fun `fails for an inner class`() {
        assertNotInstantiable(
            """
            package com.example
            import dev.slne.surf.microservice.api.Microservice
            class Outer {
                inner class PunishMicroservice : Microservice()
            }
            """,
            "inner class"
        )
    }

    @Test
    fun `fails for a Java inner class`() {
        val javaSource = SourceFile.java(
            "Outer.java",
            """
            package com.example;
            public class Outer {
                public class PunishMicroservice extends dev.slne.surf.microservice.api.Microservice {}
            }
            """
        )

        val run = ProcessorRun.compile(microserviceApi, javaSource)

        assertEquals(ExitCode.COMPILATION_ERROR, run.exitCode)
        assertContains(run.messages, "cannot be instantiated by the runtime")
        assertContains(run.messages, "inner class")
    }

    private fun compile(code: String): ProcessorRun =
        ProcessorRun.compile(microserviceApi, TestSources.kotlin("Service.kt", code))

    private fun assertMainClass(expected: String, run: ProcessorRun) {
        assertEquals(ExitCode.OK, run.exitCode, run.messages)
        assertEquals(expected, run.resource(MicroserviceResources.MAIN_CLASS_PATH))
    }

    private fun assertNotInstantiable(code: String, reason: String) {
        val run = compile(code)

        assertEquals(ExitCode.COMPILATION_ERROR, run.exitCode)
        assertContains(run.messages, "cannot be instantiated by the runtime")
        assertContains(run.messages, reason)
    }
}
