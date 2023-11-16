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

package eu.aylett.gradle.functionaltests.gitversion

import eu.aylett.gradle.gitversion.Git
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.Test
import java.util.regex.Pattern
import kotlin.io.path.appendText
import kotlin.io.path.writeText

private val DETACHED_HEAD_MODE_REGEX: Pattern =
  Pattern.compile(
    ".*:printVersionDetails\n1\\.0\\.0\n0\n[a-z0-9]{10}\n\n.*",
    Pattern.DOTALL,
  )

class GitVersionPluginCommitStateTests : GitVersionPluginTests() {
  @Test
  fun `isCleanTag should be false when repo dirty on a tag checkout`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      task printVersionDetails {
        doLast {
          println versionDetails ().isCleanTag
        }
      }

      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    dirtyContentFile.writeText("dirty-content")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersionDetails\nfalse\n"))
  }

  @Test
  fun `version details when detached HEAD mode`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      task printVersionDetails {
        doLast {
          println versionDetails ().lastTag
          println versionDetails ().commitDistance
          println versionDetails ().gitHash
          println versionDetails ().branchName
        }
      }

      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    val commitId = git.currentHeadFullHash
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    git.runGitCommand("commit", "-m", "commit 2", "--allow-empty")
    git.runGitCommand("checkout", commitId)

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    assertThat(
      buildResult.output,
      matchesRegex(DETACHED_HEAD_MODE_REGEX),
    )
  }

  @Test
  fun `version filters out tags not matching prefix and strips prefix`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion (prefix:"my-product@")
      task printVersionDetails {
        doLast {
          println versionDetails (prefix:"my-product@").lastTag
        }
      }
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "my-product@1.0.0", "-m", "my-product@1.0.0")
    git.runGitCommand("commit", "-m", "commit 2", "--allow-empty")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersionDetails\n1.0.0\n"))
  }

  @Test
  fun `git describe with commit after annotated tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "v1.0.0", "-m", "1.0.0")
    dirtyContentFile.writeText("dirty-content")
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "add some stuff")
    val commitSha = git.currentHeadFullHash

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(
      buildResult.output,
      containsString(":printVersion\n1.0.0-1-g${commitSha.substring(0, 7)}\n"),
    )
  }

  @Test
  fun `git describe with commit after lightweight tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "v1.0.0")
    dirtyContentFile.writeText("dirty-content")
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "add some stuff")
    val commitSha = git.currentHeadFullHash

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(
      buildResult.output,
      containsString(":printVersion\n1.0.0-1-g${commitSha.substring(0, 7)}\n"),
    )
  }
}
