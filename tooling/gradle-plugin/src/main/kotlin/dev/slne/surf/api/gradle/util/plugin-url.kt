package dev.slne.surf.api.gradle.util

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.of
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.net.URI
import javax.inject.Inject

/**
 * The Gradle property that sets the website of a generated plugin file.
 */
internal const val PLUGIN_URL_PROPERTY = "url"

/**
 * Returns the default website of the project's plugin file: the `url` Gradle property, else the
 * project's `origin` Git remote as an `https` URL.
 *
 * The Git remote is read only when the provider is queried and the property is absent. The
 * provider has no value outside a Git checkout, without a `git` executable, or when the remote
 * has no web form, such as a local path.
 */
internal fun Project.pluginUrl(): Provider<String> {
    return providers.gradleProperty(PLUGIN_URL_PROPERTY).orElse(
        providers.of(GitOriginUrl::class) { parameters.directory.set(layout.projectDirectory) },
    )
}

/**
 * Reads the `origin` remote of the Git repository that contains [Parameters.directory] and
 * returns it as normalized by [normalizeGitRemoteUrl].
 *
 * Any failure of `git` yields no value instead of failing the build.
 */
internal abstract class GitOriginUrl @Inject constructor(
    private val exec: ExecOperations,
) : ValueSource<String, GitOriginUrl.Parameters> {

    interface Parameters : ValueSourceParameters {
        val directory: DirectoryProperty
    }

    override fun obtain(): String? {
        val output = ByteArrayOutputStream()
        val result = runCatching {
            exec.exec {
                workingDir = parameters.directory.get().asFile
                commandLine("git", "config", "--get", "remote.origin.url")
                standardOutput = output
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
        }.getOrNull()
        if (result?.exitValue != 0) return null

        return normalizeGitRemoteUrl(output.toString(Charsets.UTF_8).trim())
    }
}

// The host has at least two characters, so a Windows path such as `C:\repo` does not match.
private val SCP_LIKE_REMOTE = Regex("""^(?:[^@/\s]+@)?([^:/\\\s]{2,}):(?!//)(.+)$""")
private val WEB_SCHEMES = setOf("https", "http", "ssh", "git", "git+ssh", "ssh+git")

/**
 * Converts a Git remote URL to the `https` URL of the repository's web page, or returns `null`
 * when [remote] has no web form.
 *
 * Accepts `https`, `http`, `ssh` and `git` URLs and the scp-like form `user@host:path`. The result
 * drops credentials, the port, a trailing `/` and the `.git` suffix, so
 * `git@github.com:org/repo.git` becomes `https://github.com/org/repo`. Local paths and `file`
 * URLs return `null`.
 */
internal fun normalizeGitRemoteUrl(remote: String): String? {
    val (host, path) = hostAndPath(remote.trim()) ?: return null

    val repositoryPath = path.trim('/').removeSuffix(".git").trimEnd('/')
    if (host.isEmpty() || repositoryPath.isEmpty()) return null

    return "https://$host/$repositoryPath"
}

private fun hostAndPath(remote: String): Pair<String, String>? {
    if ("://" !in remote) {
        val match = SCP_LIKE_REMOTE.matchEntire(remote) ?: return null

        return match.groupValues[1] to match.groupValues[2]
    }

    val uri = runCatching { URI(remote) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() !in WEB_SCHEMES) return null
    val host = uri.host ?: return null

    return host to uri.path.orEmpty()
}
