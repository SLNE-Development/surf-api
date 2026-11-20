package dev.slne.surf.api.gradle.pluginclass

import dev.slne.surf.api.gradle.platform.SurfApiPlatform
import org.gradle.api.InvalidUserDataException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BasePackageTest {

    @TempDir
    lateinit var directory: File

    private fun source(name: String, content: String): File =
        directory.resolve(name).apply { writeText(content) }

    private fun resolution(
        files: List<File>,
        explicitPackage: String?,
        platform: SurfApiPlatform = SurfApiPlatform.PAPER,
    ) = BasePackage.resolve(BasePackage.declaredPackages(files), explicitPackage, platform)

    private fun resolve(files: List<File>, explicitPackage: String?, platform: SurfApiPlatform = SurfApiPlatform.PAPER) =
        resolution(files, explicitPackage, platform).basePackage

    @Test
    fun `common package is the longest shared prefix of whole segments`() {
        val common = BasePackage.commonPackage(
            listOf("com.example.plugin.listener", "com.example.plugin", "com.example.plugin.command.admin")
        )

        assertEquals("com.example.plugin", common)
    }

    @Test
    fun `common package does not split a segment`() {
        assertEquals("com.example", BasePackage.commonPackage(listOf("com.example.pluginA", "com.example.pluginB")))
    }

    @Test
    fun `a single package is its own common package`() {
        assertEquals("com.example.plugin", BasePackage.commonPackage(listOf("com.example.plugin")))
    }

    @Test
    fun `packages without a shared prefix have no common package`() {
        assertEquals("", BasePackage.commonPackage(listOf("com.example", "org.example")))
    }

    @Test
    fun `a file in the default package makes the common package empty`() {
        assertEquals("", BasePackage.commonPackage(listOf("com.example", "")))
    }

    @Test
    fun `reads the package of Kotlin and Java files`() {
        assertEquals("com.example.kt", BasePackage.declaredPackage("package com.example.kt\n\nclass A"))
        assertEquals("com.example.java", BasePackage.declaredPackage("package com.example.java;\n\npublic class A {}"))
    }

    @Test
    fun `reads the package behind comments and file annotations`() {
        val source = """
            /* license
               package not.this.one */
            // package nor.this
            @file:Suppress("unused")
            package com.example.documented

            class A
        """.trimIndent()

        assertEquals("com.example.documented", BasePackage.declaredPackage(source))
    }

    @Test
    fun `reads a backticked package`() {
        assertEquals("com.example.in", BasePackage.declaredPackage("package com.example.`in`\n"))
    }

    @Test
    fun `a file without a package declaration is in the default package`() {
        assertEquals("", BasePackage.declaredPackage("class A"))
    }

    @Test
    fun `resolves the common package of mixed Java and Kotlin sources`() {
        val files = listOf(
            source("Main.kt", "package com.example.plugin\nclass Main"),
            source("Util.java", "package com.example.plugin.util;\nclass Util {}"),
            source("Listener.kt", "package com.example.plugin.listener\nclass Listener"),
        )

        assertEquals("com.example.plugin", resolve(files, null))
    }

    @Test
    fun `resolves a single source file`() {
        val files = listOf(source("Main.kt", "package com.example.solo\nclass Main"))

        assertEquals("com.example.solo", resolve(files, null))
    }

    @Test
    fun `an explicit package wins over the sources`() {
        val files = listOf(source("Main.kt", "package com.example.plugin\nclass Main"))

        assertEquals("com.example", resolve(files, "com.example"))
    }

    @Test
    fun `an explicit package is accepted without sources`() {
        assertEquals("com.example", resolve(emptyList(), "com.example"))
    }

    @Test
    fun `rejects sources that share no package`() {
        val files = listOf(
            source("A.kt", "package com.example\nclass A"),
            source("B.kt", "package org.example\nclass B"),
        )

        val failure = assertFailsWith<InvalidUserDataException> { resolve(files, null) }

        assertTrue("share no common package" in failure.message.orEmpty(), failure.message)
        assertTrue("basePackage" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `rejects missing sources`() {
        val failure = assertFailsWith<InvalidUserDataException> { resolve(emptyList(), null) }

        assertTrue("no Kotlin or Java sources" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `rejects surf-api and its parents`() {
        for (reserved in listOf("dev.slne.surf.api", "dev.slne.surf", "dev.slne", "dev")) {
            val failure = assertFailsWith<InvalidUserDataException>(reserved) {
                resolve(emptyList(), reserved)
            }

            assertTrue("surf-api" in failure.message.orEmpty(), failure.message)
        }
    }

    @Test
    fun `says that surf-api's own package is rejected and that a parent would scan surf-api`() {
        val own = assertFailsWith<InvalidUserDataException> { resolve(emptyList(), "dev.slne.surf.api") }
        val parent = assertFailsWith<InvalidUserDataException> { resolve(emptyList(), "dev.slne.surf") }

        assertTrue("is surf-api's own package" in own.message.orEmpty(), own.message)
        assertTrue("contains surf-api's package 'dev.slne.surf.api'" in parent.message.orEmpty(), parent.message)
        assertTrue("would include the components of surf-api itself" in parent.message.orEmpty(), parent.message)
    }

    @Test
    fun `Minestom rejects a package inside surf-api because it shares the class loader`() {
        val failure = assertFailsWith<InvalidUserDataException> {
            resolve(emptyList(), "dev.slne.surf.api.example", SurfApiPlatform.MINESTOM)
        }

        assertTrue("lies inside surf-api's package" in failure.message.orEmpty(), failure.message)
        assertTrue("shares the class loader" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `Paper and Velocity accept a package inside surf-api because they have a class loader of their own`() {
        for (platform in listOf(SurfApiPlatform.PAPER, SurfApiPlatform.VELOCITY)) {
            assertEquals("dev.slne.surf.api.example", resolve(emptyList(), "dev.slne.surf.api.example", platform))
        }
    }

    @Test
    fun `rejects a package that is no Java package name`() {
        for (malformed in listOf("com.example.", "com..example", "com.example.class", "1com.example")) {
            val failure = assertFailsWith<InvalidUserDataException>(malformed) { resolve(emptyList(), malformed) }

            assertTrue("is not a valid package name" in failure.message.orEmpty(), failure.message)
        }
    }

    @Test
    fun `an explicit package that does not contain every source package is accepted with a warning`() {
        val files = listOf(
            source("Core.kt", "package com.example.plugin.core\nclass Core"),
            source("Listener.kt", "package com.example.plugin.listener\nclass Listener"),
        )

        val resolution = resolution(files, "com.example.plugin.core")

        assertEquals("com.example.plugin.core", resolution.basePackage)
        val warning = resolution.warnings.single()
        assertTrue("'com.example.plugin.listener'" in warning, warning)
        assertTrue("'com.example.plugin.core'" !in warning.substringAfter("source packages"), warning)
    }

    @Test
    fun `an explicit package that contains the sources raises no warning`() {
        val files = listOf(
            source("Core.kt", "package com.example.plugin.core\nclass Core"),
            source("Listener.kt", "package com.example.plugin.listener\nclass Listener"),
        )

        assertEquals(emptyList(), resolution(files, "com.example.plugin").warnings)
        assertEquals(emptyList(), resolution(files, null).warnings)
    }

    @Test
    fun `the package of a class name is everything before the last dot`() {
        assertEquals("com.example.plugin", BasePackage.packageOf("com.example.plugin.Main"))
        assertEquals("", BasePackage.packageOf("Main"))
    }

    @Test
    fun `a scanned class that contains every source package raises no warning`() {
        val warnings = BasePackage.checkScanned(
            "com.example.plugin.Boot",
            listOf("com.example.plugin", "com.example.plugin.listener"),
            SurfApiPlatform.PAPER,
        )

        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `a scanned class without sources raises no warning`() {
        assertEquals(emptyList(), BasePackage.checkScanned("com.example.Main", emptyList(), SurfApiPlatform.VELOCITY))
    }

    @Test
    fun `a scanned class in a narrower package warns about the sources the scan misses`() {
        val warnings = BasePackage.checkScanned(
            "com.example.plugin.core.Boot",
            listOf("com.example.plugin.core", "com.example.plugin.listener", "org.other"),
            SurfApiPlatform.PAPER,
        )

        val warning = warnings.single()
        assertTrue("'com.example.plugin.core'" in warning.substringBefore("does not contain"), warning)
        assertTrue("'com.example.plugin.listener', 'org.other'" in warning, warning)
    }

    @Test
    fun `a scanned class in the default package is rejected`() {
        val failure = assertFailsWith<InvalidUserDataException> {
            BasePackage.checkScanned("Main", emptyList(), SurfApiPlatform.VELOCITY)
        }

        assertTrue("default package" in failure.message.orEmpty(), failure.message)
    }

    @Test
    fun `a Minestom class inside surf-api is rejected while a Paper class there is scanned`() {
        assertFailsWith<InvalidUserDataException> {
            BasePackage.checkScanned("dev.slne.surf.api.example.Main", emptyList(), SurfApiPlatform.MINESTOM)
        }

        assertEquals(
            emptyList(),
            BasePackage.checkScanned("dev.slne.surf.api.example.Boot", emptyList(), SurfApiPlatform.PAPER),
        )
    }

    @Test
    fun `a scanned class in surf-api's own package or a parent of it is rejected`() {
        for (className in listOf("dev.slne.surf.api.Main", "dev.slne.surf.Main", "dev.slne.Main")) {
            assertFailsWith<InvalidUserDataException>(className) {
                BasePackage.checkScanned(className, emptyList(), SurfApiPlatform.PAPER)
            }
        }
    }

    @Test
    fun `rejects a computed package that is a parent of surf-api`() {
        val files = listOf(
            source("A.kt", "package dev.slne.surf.example\nclass A"),
            source("B.kt", "package dev.slne.survival\nclass B"),
        )

        assertFailsWith<InvalidUserDataException> { resolve(files, null) }
    }

    @Test
    fun `accepts a package beside surf-api`() {
        assertEquals("dev.slne.surf.example", resolve(emptyList(), "dev.slne.surf.example"))
        assertEquals("dev.slne.survival", resolve(emptyList(), "dev.slne.survival"))
    }
}
