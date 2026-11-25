package dev.slne.surf.api.gradle.util

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NormalizeGitRemoteUrlTest {

    @ParameterizedTest
    @CsvSource(
        "https://github.com/org/repo.git, https://github.com/org/repo",
        "https://github.com/org/repo, https://github.com/org/repo",
        "https://github.com/org/repo/, https://github.com/org/repo",
        "http://git.example.com/org/repo.git, https://git.example.com/org/repo",
        "https://x-access-token:secret@github.com/org/repo.git, https://github.com/org/repo",
        "git@github.com:org/repo.git, https://github.com/org/repo",
        "github.com:org/repo, https://github.com/org/repo",
        "ssh://git@github.com/org/repo.git, https://github.com/org/repo",
        "ssh://git@git.example.com:2222/group/sub/repo.git, https://git.example.com/group/sub/repo",
        "git://github.com/org/repo.git, https://github.com/org/repo",
    )
    fun `converts a remote to its web url`(remote: String, expected: String) {
        assertEquals(expected, normalizeGitRemoteUrl(remote))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "/srv/git/repo.git",
            "../repo",
            "C:\\repos\\repo",
            "file:///srv/git/repo.git",
            "https://github.com/",
        ],
    )
    fun `returns null for a remote without a web url`(remote: String) {
        assertNull(normalizeGitRemoteUrl(remote))
    }
}
