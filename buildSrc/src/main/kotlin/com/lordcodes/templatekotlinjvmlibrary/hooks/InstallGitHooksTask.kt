package com.lordcodes.templatekotlinjvmlibrary.hooks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

open class InstallGitHooksTask : DefaultTask() {
    @Suppress("unused")
    @TaskAction
    fun installGitHooks() {
        val gitHooksDirectory = try {
            resolveGitHooksDirectory()
        } catch (error: IOException) {
            System.err.println(error.message)
            exitProcess(1)
        }
        val projectHooksDirectory = try {
            resolveProjectHooksDirectory()
        } catch (error: IOException) {
            System.err.println(error.message)
            exitProcess(1)
        }

        HOOK_FILES.forEach { name ->
            val hookFile = File(gitHooksDirectory, name)
            installHook(hookFile) {
                File(projectHooksDirectory, TEMPLATE_FILE).readText()
            }
        }
    }

    @Throws(IOException::class)
    private fun resolveGitHooksDirectory(): File {
        val gitDirectory = File(".git")
        if (!gitDirectory.isDirectory) {
            throw IOException("Couldn't find .git directory. Please make sure you are in the project root directory.")
        }

        val hooksDirectory = gitDirectory.resolve("hooks")
        if (!hooksDirectory.exists() && !hooksDirectory.mkdir()) {
            throw IOException("Failed to create .git/hooks directory.")
        }
        return hooksDirectory
    }

    @Throws(IOException::class)
    private fun resolveProjectHooksDirectory(): File {
        val hooksDirectory = File(PROJECT_HOOKS_DIRECTORY)
        if (!hooksDirectory.isDirectory && !hooksDirectory.mkdir()) {
            throw IOException("Failed to find or create .git-hooks directory.")
        }
        return hooksDirectory
    }

    private fun installHook(hookFile: File, hookContent: () -> String) {
        if (hookFile.exists()) {
            hookFile.handleExisting()
        } else {
            hookFile.createNewFile()
        }
        hookFile.writeText(hookContent())
        hookFile.appendText(UPDATEABLE_IDENTIFIER)
        hookFile.setExecutable(true, false)
    }

    private fun File.handleExisting() {
        if (isUserGeneratedHook()) {
            val backupFile = File("${this.absolutePath}.backup")
            copyTo(backupFile)
        } else {
            delete()
        }
    }

    private fun File.isUserGeneratedHook(): Boolean {
        if (!exists()) {
            return false
        }
        return readText().indexOf(UPDATEABLE_IDENTIFIER) == -1
    }

    companion object {
        private const val PROJECT_HOOKS_DIRECTORY = ".git-hooks"
        private const val TEMPLATE_FILE = "hook-run-all-template.sh"
        const val UPDATEABLE_IDENTIFIER = "### Auto-generated by template-kotlin-jvm-library ###"

        val HOOK_FILES = listOf(
            "applypatch-msg",
            "pre-applypatch",
            "post-applypatch",
            "pre-commit",
            "prepare-commit-msg",
            "commit-msg",
            "post-commit",
            "pre-rebase",
            "post-checkout",
            "post-merge",
            "pre-push",
            "pre-receive",
            "update",
            "post-receive",
            "post-update",
            "push-to-checkout",
            "pre-auto-gc",
            "post-rewrite",
            "sendemail-validate",
            "fsmonitor-watchman",
            "p4-pre-submit",
            "post-index-change"
        )
    }
}
