package dev.slne.surf.nats.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConnectRulesTest {

    @Test
    fun serversAreTrimmedAndEmptyEntriesDropped() {
        assertEquals(listOf("nats://a:4222", "nats://b:4222"), ConnectRules.parseServers(" nats://a:4222 ,, nats://b:4222 , "))
        assertEquals(emptyList(), ConnectRules.parseServers(" , "))
        assertEquals(listOf("nats://a:4222"), ConnectRules.servers(listOf("", " nats://a:4222 ")))
    }

    @Test
    fun emptyAuthValuesAreUnset() {
        assertNull(ConnectRules.authProblem(null, null, null, null))
        assertNull(ConnectRules.authProblem("", "", "", ""))
        assertNull(ConnectRules.authProblem("token", "", "", null))
        assertNull(ConnectRules.authProblem("", "user", "pass", ""))
        assertNull(ConnectRules.authProblem(null, null, null, "/creds"))
    }

    @Test
    fun conflictingAuthIsReported() {
        val together = "NATS auth: username and password must be set together"
        assertEquals(together, ConnectRules.authProblem(null, "user", null, null))
        assertEquals(together, ConnectRules.authProblem(null, "user", "", null))
        assertEquals(together, ConnectRules.authProblem(null, null, "pass", null))

        val onlyOne = "NATS auth: only one of token, username/password or credentials-file may be set"
        assertEquals(onlyOne, ConnectRules.authProblem("token", "user", "pass", null))
        assertEquals(onlyOne, ConnectRules.authProblem("token", null, null, "/creds"))
        assertEquals(onlyOne, ConnectRules.authProblem(null, "user", "pass", "/creds"))
    }
}
