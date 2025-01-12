/*
 * Copyright 2023 Andrew Aylett
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

import org.gradle.api.Action
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.problems.internal.impl.logger
import org.gradle.process.ExecSpec
import java.nio.file.Path
import kotlin.io.path.isDirectory

@Suppress("UnstableApiUsage")
class Git(
  private val gitDir: Path,
  private val providers: ProviderFactory,
) {
  init {
    if (!gitDir.isDirectory()) {
      throw UnsupportedOperationException(
        "Cannot find project repository.",
      )
    }
  }

  private fun exec(action: Action<in ExecSpec?>?): String {
    var spec: ExecSpec? = null
    val result =
      providers.exec {
        val base = this
        spec = this
        isIgnoreExitValue = true
        workingDir(gitDir.toFile())
        environment("GIT_TERMINAL_PROMPT", "0")
        environment("GIT_OPTIONAL_LOCKS", "0")
        environment("GIT_CONFIG_GLOBAL", "/dev/null")
        environment("GIT_CONFIG_SYSTEM", "/dev/null")
        action?.execute(
          object : ExecSpec by this {
            override fun commandLine(vararg args: Any?): ExecSpec {
              val withConfiguration =
                listOf(
                  args[0],
                  "-c",
                  "user.email=no@example.com",
                  "-c",
                  "user.name=no_name",
                  "-c",
                  "core.fsmonitor=false",
                  "-c",
                  "core.untrackedCache=false",
                  "-c",
                  "init.defaultBranch=main",
                ) + args.drop(1)
              return base.commandLine(*withConfiguration.toTypedArray())
            }
          },
        )
      }
    if (result.result.get().exitValue == 0) {
      val output =
        result.standardOutput.asText
          .get()
          .trim()
      logger.debug("Git command output: {}", output)
      return output
    } else {
      throw GitException(
        result.standardError.asText
          .get()
          .trim(),
        result.result.get().exitValue,
        spec,
      )
    }
  }

  val currentBranch: String by lazy {
    exec {
      commandLine("git", "branch", "--show-current")
    }
  }
  val currentHeadFullHash: String by lazy {
    assertHead()
    exec {
      commandLine("git", "rev-parse", "HEAD")
    }
  }
  val isClean: Boolean by lazy {
    exec {
      commandLine("git", "status", "--porcelain")
    }.isEmpty()
  }

  fun describe(prefix: String): String {
    assertHead()
    return exec {
      workingDir(gitDir.toFile())
      commandLine(
        "git",
        "describe",
        "--tags",
        "--always",
        "--first-parent",
        "--abbrev=7",
        "--match=$prefix*",
        "HEAD",
      )
    }
  }

  private fun assertHead() {
    exec {
      commandLine("git", "show-ref", "-q", "--verify", "--", "HEAD")
    }
  }
}
