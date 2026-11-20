import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

private const val NATS_RELOCATION_PREFIX = "dev.slne.surf.nats.libs"

/**
 * Relocates jnats and the BouncyCastle it links against below `dev.slne.surf.nats.libs`.
 *
 * jnats signs NKey challenges through BouncyCastle's lightweight API, so both packages move together.
 * BouncyCastle's native libraries are left out: JNI binds them to the original class names, and without
 * them BouncyCastle uses its Java implementation.
 */
fun ShadowJar.relocateNats() {
    relocate("io.nats", "$NATS_RELOCATION_PREFIX.io.nats")
    relocate("org.bouncycastle", "$NATS_RELOCATION_PREFIX.org.bouncycastle")
    exclude("native/**/libbc-*")
}
