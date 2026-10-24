plugins {
    id("ca.stellardrift.gitpatcher") version "2.0.0"
}

repositories {
    mavenCentral()
}

gitPatcher {
    patchedRepos {
        register("LuckPerms") {
            submodule = "LuckPerms-Upstream"
            target = file("LuckPerms")
            patches = file("LuckPerms-Patches")
        }
    }
}

abstract class PreparePatchedRepo : DefaultTask() {

    @get:Internal
    abstract val repo: DirectoryProperty

    @get:Internal
    abstract val upstream: DirectoryProperty

    @TaskAction
    fun prepare() {
        val repoDir = this.repo.get().asFile
        val commands = mutableListOf<List<String>>()

        if (!repoDir.resolve(".git").isDirectory) {
            repoDir.mkdirs()
            commands += listOf("git", "init", "--quiet")
            commands += listOf("git", "remote", "add", "origin", this.upstream.get().asFile.absolutePath)
        }

        commands += listOf("git", "config", "commit.gpgsign", "false")

        for (command in commands) {
            val process = ProcessBuilder(command)
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            check(process.waitFor() == 0) { "${command.joinToString(" ")} failed: $output" }
        }
    }

}

val preparePatchedRepo = tasks.register<PreparePatchedRepo>("preparePatchedRepo") {
    repo = layout.projectDirectory.dir("LuckPerms")
    upstream = layout.projectDirectory.dir("LuckPerms-Upstream")
}

tasks.named("applyLuckPermsPatches") {
    dependsOn(preparePatchedRepo)
}
