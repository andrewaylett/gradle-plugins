/*
 * Copyright 2023 Andrew Aylett
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.aylett.gradle.gitversion

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class NativeGit(private val directory: Path) {
  val home = Files.createTempDirectory("isolatedGitHome")

  init {
    logger.trace("Initialising git support")
    if (!gitCommandExists()) {
      throw GitException("Git not found in project")
    }
    setIsolatedConfiguration()
    if (!checkIfUserIsSet()) {
      setGitUser()
    }
  }

  private fun runGitCmd(vararg commands: String): String {
    return runGitCmd(HashMap(), *commands)
  }

  private fun runGitCmd(
    envvars: Map<String, String>,
    vararg commands: String,
  ): String {
    val cmdInput = mutableListOf("git", *commands)
    val pb = ProcessBuilder(cmdInput)
    val environment = pb.environment()
    environment.clear()
    environment["HOME"] = home.toFile().canonicalPath
    environment["GIT_TERMINAL_PROMPT"] = "0"
    environment["GIT_OPTIONAL_LOCKS"] = "0"
    environment.putAll(envvars)
    pb.directory(directory.toFile())
    pb.redirectErrorStream(true)
    val process = pb.start()
    logger.info("Running ${cmdInput.joinToString(" ")} as pid ${process.pid()}")
    val reader = process.inputReader(StandardCharsets.UTF_8)
    val builder = StringWriter()
    var interrupted = false
    while (true) {
      try {
        while (reader.ready()) {
          builder.write(reader.read())
        }
        if (process.waitFor(10, TimeUnit.MILLISECONDS)) {
          break
        }
      } catch (e: InterruptedException) {
        logger.debug("Interrupted", e)
        interrupted = true
      }
    }
    reader.transferTo(builder)
    val exitCode = process.exitValue()
    logger.debug("Git command pid ${process.pid()} completed")
    if (interrupted) {
      throw GitException("Interrupted when trying to run ${cmdInput.joinToString(" ")}")
    }
    if (exitCode != 0) {
      throw GitException(
        "Failed to run ${cmdInput.joinToString(" ")}, output ${builder.toString().trim()}",
        exitCode,
      )
    }

    return builder.toString().trim { it <= ' ' }
  }

  fun runGitCommand(
    envvar: Map<String, String>,
    vararg command: String,
  ): String = runGitCmd(envvar, *command)

  fun runGitCommand(vararg command: String): String = runGitCommand(HashMap(), *command)

  private fun checkIfUserIsSet(): Boolean {
    try {
      return runGitCmd("config", "user.email").isNotEmpty()
    } catch (e: GitException) {
      if (e.statusCode == 1) {
        // Per the man page, section is invalid
        return false
      }
      throw e
    }
  }

  private fun setGitUser() {
    runGitCommand("config", "--global", "user.email", "email@example.com")
    runGitCommand("config", "--global", "user.name", "name")
  }

  private fun setIsolatedConfiguration() {
    runGitCommand("config", "--global", "init.defaultBranch", "main")
    runGitCommand("config", "--global", "core.fsmonitor", "false")
    runGitCommand("config", "--global", "core.untrackedCache", "false")
  }

  val currentBranch: String
    get() = runGitCmd("branch", "--show-current")

  val currentHeadFullHash: String
    get() = runGitCmd("rev-parse", "HEAD")

  val isClean: Boolean
    get() = runGitCmd("status", "--porcelain").isEmpty()

  fun describe(prefix: String): String =
    runGitCmd(
      "describe",
      "--tags",
      "--always",
      "--first-parent",
      "--abbrev=7",
      "--match=$prefix*",
      "HEAD",
    )

  private fun gitCommandExists(): Boolean {
    return try {
      // verify that "git" command exists (throws exception if it does not)
      val gitVersionProcess = ProcessBuilder("git", "version").start()
      check(gitVersionProcess.waitFor() == 0) { "error invoking git command" }
      true
    } catch (e: IOException) {
      log.error("Native git command not found", e)
      false
    } catch (e: InterruptedException) {
      log.error("Native git command not found", e)
      false
    } catch (e: RuntimeException) {
      log.error("Native git command not found", e)
      false
    }
  }

  private val log by lazy { LoggerFactory.getLogger(NativeGit::class.java) }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger(NativeGit::class.java)
  }
}
