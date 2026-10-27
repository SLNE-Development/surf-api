package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.ConnectOptions
import dev.slne.surf.nats.js.client.ConnectSettings
import js.objects.unsafeJso
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectSettingsTest {

    private fun settings(options: ConnectOptions): ConnectSettings = ConnectSettings.of(options)

    private fun failure(options: ConnectOptions): String {
        return assertFailsWith<IllegalArgumentException> { settings(options) }.message!!
    }

    @Test
    fun defaults() {
        val settings = settings(ConnectOptions(name = "surf-panel"))
        assertEquals("surf-panel", settings.name)
        assertEquals(listOf("nats://localhost:4222"), settings.servers)
        assertNull(settings.auth)
        assertEquals(5000, settings.connectTimeoutMs)
    }

    @Test
    fun clientNameMustBeKebabCase() {
        assertTrue(failure(ConnectOptions(name = "Surf_Panel")).contains("must be kebab-case"))
        assertTrue(failure(ConnectOptions(name = "surf--panel")).contains("must be kebab-case"))
        // JS callers may omit the name although the type requires it
        assertTrue(failure(unsafeJso<ConnectOptions>()).contains("client name is required"))
    }

    @Test
    fun servers() {
        assertEquals(
            listOf("nats://a:4222", "nats://b:4222"),
            settings(ConnectOptions(name = "c", servers = arrayOf(" nats://a:4222", "nats://b:4222 ", ""))).servers,
        )
        assertEquals(
            listOf("nats://a:4222"),
            settings(ConnectOptions(name = "c", servers = arrayOf("nats://a:4222", " "))).servers,
        )
        assertTrue(failure(ConnectOptions(name = "c", servers = arrayOf(" ", ""))).contains("must not be empty"))
        assertTrue(failure(ConnectOptions(name = "c", servers = arrayOf())).contains("must not be empty"))
    }

    @Test
    fun authMethods() {
        val tokenAuth = assertIs<ConnectSettings.Auth.Token>(settings(ConnectOptions(name = "c", token = "t")).auth)
        assertEquals("t", tokenAuth.token)

        val userPass = assertIs<ConnectSettings.Auth.UserPass>(
            settings(ConnectOptions(name = "c", user = "u", pass = "p")).auth,
        )
        assertEquals("u", userPass.user)
        assertEquals("p", userPass.pass)

        val creds = assertIs<ConnectSettings.Auth.CredsFile>(
            settings(ConnectOptions(name = "c", credsFile = "/x.creds")).auth,
        )
        assertEquals("/x.creds", creds.path)
    }

    @Test
    fun authRules() {
        assertTrue(failure(ConnectOptions(name = "c", user = "u")).contains("must be set together"))
        assertTrue(failure(ConnectOptions(name = "c", pass = "p")).contains("must be set together"))
        assertTrue(failure(ConnectOptions(name = "c", token = "t", user = "u", pass = "p")).contains("only one of"))
        assertTrue(failure(ConnectOptions(name = "c", token = "t", credsFile = "/x")).contains("only one of"))
    }

    @Test
    fun emptyStringsAreUnset() {
        assertNull(settings(ConnectOptions(name = "c", token = "", user = "", pass = "", credsFile = "")).auth)
        assertIs<ConnectSettings.Auth.Token>(settings(ConnectOptions(name = "c", token = "t", user = "")).auth)
    }

    @Test
    fun connectTimeoutMustBePositive() {
        assertEquals(250, settings(ConnectOptions(name = "c", connectTimeoutMs = 250)).connectTimeoutMs)
        assertTrue(failure(ConnectOptions(name = "c", connectTimeoutMs = 0)).contains("must be positive"))
    }
}
