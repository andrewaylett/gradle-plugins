/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.gitversion

import com.google.common.annotations.VisibleForTesting
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Arrays

internal class Git
  @VisibleForTesting
  constructor(directory: File, testing: Boolean) {
    private val directory: File
    private val testing: Boolean

    constructor(directory: File) : this(directory, false)

    constructor(directory: Path, testing: Boolean) : this(directory.toFile(), testing)

    init {
      if (!gitCommandExists()) {
        throw RuntimeException("Git not found in project")
      }
      this.directory = directory
      this.testing = testing
      if (testing && !checkIfUserIsSet()) {
        setGitUser()
      }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun runGitCmd(vararg commands: String): String {
      return runGitCmd(HashMap(), *commands)
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun runGitCmd(
      envvars: Map<String, String>,
      vararg commands: String,
    ): String {
      val cmdInput: MutableList<String> = ArrayList()
      cmdInput.add("git")
      cmdInput.addAll(Arrays.asList(*commands))
      val pb = ProcessBuilder(cmdInput)
      val environment = pb.environment()
      environment.putAll(envvars)
      if (this.testing) {
        environment.put("HOME", directory.canonicalPath)
      }
      pb.directory(directory)
      pb.redirectErrorStream(true)
      val process = pb.start()
      val reader = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
      val builder = StringBuilder()
      var line: String?
      while (reader.readLine().also { line = it } != null) {
        builder.append(line)
        builder.append(System.getProperty("line.separator"))
      }
      val exitCode = process.waitFor()
      return if (exitCode != 0) {
        ""
      } else {
        builder.toString().trim { it <= ' ' }
      }
    }

    fun runGitCommand(
      envvar: Map<String, String>,
      vararg command: String,
    ): String? {
      return try {
        runGitCmd(envvar, *command)
      } catch (e: IOException) {
        log.debug("Native git command {} failed.\n", command, e)
        null
      } catch (e: InterruptedException) {
        log.debug("Native git command {} failed.\n", command, e)
        null
      } catch (e: RuntimeException) {
        log.debug("Native git command {} failed.\n", command, e)
        null
      }
    }

    fun runGitCommand(vararg command: String): String? {
      return runGitCommand(HashMap(), *command)
    }

    private fun checkIfUserIsSet(): Boolean {
      return try {
        val userEmail = runGitCmd("config", "user.email")
        userEmail.isNotEmpty()
      } catch (e: IOException) {
        log.debug("Native git config user.email failed", e)
        false
      } catch (e: InterruptedException) {
        log.debug("Native git config user.email failed", e)
        false
      } catch (e: RuntimeException) {
        log.debug("Native git config user.email failed", e)
        false
      }
    }

    private fun setGitUser() {
      try {
        runGitCommand("config", "--global", "user.email", "email@example.com")
        runGitCommand("config", "--global", "user.name", "name")
      } catch (e: RuntimeException) {
        log.debug("Native git set user failed", e)
      }
    }

    val currentBranch: String?
      get() =
        try {
          val branch = runGitCmd("branch", "--show-current")
          branch.ifEmpty {
            null
          }
        } catch (e: IOException) {
          log.debug("Native git branch --show-current failed", e)
          null
        } catch (e: InterruptedException) {
          log.debug("Native git branch --show-current failed", e)
          null
        } catch (e: RuntimeException) {
          log.debug("Native git branch --show-current failed", e)
          null
        }
    val currentHeadFullHash: String?
      get() =
        try {
          runGitCmd("rev-parse", "HEAD")
        } catch (e: IOException) {
          log.debug("Native git rev-parse HEAD failed", e)
          null
        } catch (e: InterruptedException) {
          log.debug("Native git rev-parse HEAD failed", e)
          null
        } catch (e: RuntimeException) {
          log.debug("Native git rev-parse HEAD failed", e)
          null
        }
    val isClean: Boolean?
      get() =
        try {
          val result = runGitCmd("status", "--porcelain")
          result.isEmpty()
        } catch (e: IOException) {
          log.debug("Native git status --porcelain failed", e)
          null
        } catch (e: InterruptedException) {
          log.debug("Native git status --porcelain failed", e)
          null
        } catch (e: RuntimeException) {
          log.debug("Native git status --porcelain failed", e)
          null
        }

    fun describe(prefix: String): String? {
      return try {
        val result =
          runGitCmd(
            "describe",
            "--tags",
            "--always",
            "--first-parent",
            "--abbrev=7",
            "--match=$prefix*",
            "HEAD",
          )
        result.ifEmpty {
          null
        }
      } catch (e: IOException) {
        log.debug("Native git describe failed", e)
        null
      } catch (e: InterruptedException) {
        log.debug("Native git describe failed", e)
        null
      } catch (e: RuntimeException) {
        log.debug("Native git describe failed", e)
        null
      }
    }

    private fun gitCommandExists(): Boolean {
      return try {
        // verify that "git" command exists (throws exception if it does not)
        val gitVersionProcess = ProcessBuilder("git", "version").start()
        check(gitVersionProcess.waitFor() == 0) { "error invoking git command" }
        true
      } catch (e: IOException) {
        log.debug("Native git command not found", e)
        false
      } catch (e: InterruptedException) {
        log.debug("Native git command not found", e)
        false
      } catch (e: RuntimeException) {
        log.debug("Native git command not found", e)
        false
      }
    }

    companion object {
      private val log = LoggerFactory.getLogger(Git::class.java)
    }
  }
