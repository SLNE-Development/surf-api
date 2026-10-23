package dev.slne.surf.nats.js

import js.objects.unsafeJso
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectSettingsTest {

    private fun settings(init: ConnectOptionsInit.() -> Unit): ConnectSettings = ConnectSettings.of(unsafeJso(init))

    private fun failure(init: ConnectOptionsInit.() -> Unit): String {
        return assertFailsWith<IllegalArgumentException> { settings(init) }.message!!
    }

    @Test
    fun defaults() {
        val settings = settings { name = "surf-panel" }
        assertEquals("surf-panel", settings.name)
        assertEquals(listOf("nats://localhost:4222"), settings.servers)
        assertNull(settings.auth)
        assertEquals(5000, settings.connectTimeoutMs)
    }

    @Test
    fun clientNameMustBeKebabCase() {
        assertTrue(failure { name = "Surf_Panel" }.contains("must be kebab-case"))
        assertTrue(failure { name = "surf--panel" }.contains("must be kebab-case"))
        assertTrue(failure { }.contains("client name is required"))
    }

    @Test
    fun servers() {
        assertEquals(
            listOf("nats://a:4222", "nats://b:4222"),
            settings {
                name = "c"
                servers = arrayOf(" nats://a:4222", "nats://b:4222 ", "")
            }.servers,
        )
        assertEquals(
            listOf("nats://a:4222"),
            settings {
                name = "c"
                servers = arrayOf("nats://a:4222", " ")
            }.servers,
        )
        assertTrue(failure {
            name = "c"
            servers = arrayOf(" ", "")
        }.contains("must not be empty"))
        assertTrue(failure {
            name = "c"
            servers = arrayOf()
        }.contains("must not be empty"))
    }

    @Test
    fun authMethods() {
        val tokenAuth = assertIs<ConnectSettings.Auth.Token>(settings {
            name = "c"
            token = "t"
        }.auth)
        assertEquals("t", tokenAuth.token)

        val userPass = assertIs<ConnectSettings.Auth.UserPass>(settings {
            name = "c"
            user = "u"
            pass = "p"
        }.auth)
        assertEquals("u", userPass.user)
        assertEquals("p", userPass.pass)

        val creds = assertIs<ConnectSettings.Auth.CredsFile>(settings {
            name = "c"
            credsFile = "/x.creds"
        }.auth)
        assertEquals("/x.creds", creds.path)
    }

    @Test
    fun authRules() {
        assertTrue(failure {
            name = "c"
            user = "u"
        }.contains("must be set together"))
        assertTrue(failure {
            name = "c"
            pass = "p"
        }.contains("must be set together"))
        assertTrue(failure {
            name = "c"
            token = "t"
            user = "u"
            pass = "p"
        }.contains("only one of"))
        assertTrue(failure {
            name = "c"
            token = "t"
            credsFile = "/x"
        }.contains("only one of"))
    }

    @Test
    fun emptyStringsAreUnset() {
        assertNull(settings {
            name = "c"
            token = ""
            user = ""
            pass = ""
            credsFile = ""
        }.auth)
        assertIs<ConnectSettings.Auth.Token>(settings {
            name = "c"
            token = "t"
            user = ""
        }.auth)
    }

    @Test
    fun connectTimeoutMustBePositive() {
        assertEquals(250, settings {
            name = "c"
            connectTimeoutMs = 250
        }.connectTimeoutMs)
        assertTrue(failure {
            name = "c"
            connectTimeoutMs = 0
        }.contains("must be positive"))
    }
}
