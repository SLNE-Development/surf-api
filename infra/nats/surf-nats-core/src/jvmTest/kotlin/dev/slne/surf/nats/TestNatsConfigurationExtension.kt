package dev.slne.surf.nats

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Installs the test `config.yml` before the first test class runs, so that classes which read the
 * NATS configuration without calling [TestNats.load] find it.
 */
class TestNatsConfigurationExtension : BeforeAllCallback {

    override fun beforeAll(context: ExtensionContext) {
        TestNats.installConfiguration()
    }
}
