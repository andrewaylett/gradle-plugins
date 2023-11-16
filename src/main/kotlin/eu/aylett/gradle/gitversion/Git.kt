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

import com.google.common.annotations.VisibleForTesting
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class Git
  @VisibleForTesting
  constructor(private val directory: Path, private val testing: Boolean) {
    constructor(directory: Path) : this(directory, false)

    init {
      if (!gitCommandExists()) {
        throw GitException("Git not found in project")
      }
      if (testing) {
        setDefaultBranchToMain()
        if (!checkIfUserIsSet()) {
          setGitUser()
        }
      }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun runGitCmd(vararg commands: String): String {
      return runGitCmd(HashMap(), *commands)
    }

    @Throws(IOException::class)
    private fun runGitCmd(
      envvars: Map<String, String>,
      vararg commands: String,
    ): String {
      val cmdInput = mutableListOf("git", *commands)
      val pb = ProcessBuilder(cmdInput)
      val environment = pb.environment()
      environment.putAll(envvars)
      if (this.testing) {
        environment["HOME"] = directory.toFile().canonicalPath
      }
      pb.directory(directory.toFile())
      pb.redirectErrorStream(true)
      val process = pb.start()
      val reader = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
      val builder = StringBuilder()
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        builder.append(line)
        builder.append(System.getProperty("line.separator"))
      }
      var exitCode: Int
      var interrupted = false
      while (true) {
        try {
          exitCode = process.waitFor()
          break
        } catch (e: InterruptedException) {
          interrupted = true
        }
      }
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

    private fun setDefaultBranchToMain() {
      runGitCommand("config", "--global", "init.defaultBranch", "main")
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

    private val log by lazy { LoggerFactory.getLogger(Git::class.java) }
  }
