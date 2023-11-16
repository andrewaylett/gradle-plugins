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

private val MERGE_COMMIT_REGEX: Pattern =
  Pattern.compile(
    ".*:printVersion\n1\\.0\\.0-1-g[a-z0-9]{7}\n.*",
    Pattern.DOTALL,
  )

class GitVersionPluginDescribeTests : GitVersionPluginTests() {
  @Test
  fun `unspecified when no tags are present`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )

    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersion\nunspecified\n"))
  }

  @Test
  fun `git describe when annotated tag is present`() {
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
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersion\n1.0.0\n"))
  }

  @Test
  fun `git describe when lightweight tag is present`() {
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
    git.runGitCommand("tag", "1.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersion\n1.0.0\n"))
  }

  @Test
  fun `git describe when annotated tag is present with merge commit`() {
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

    // create repository with a single commit tagged as 1.0.0
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // create a new branch called "hotfix" that has a single commit and is tagged with "1.0.0-hotfix"
    val master = git.currentHeadFullHash.substring(0, 7)
    git.runGitCommand("checkout", "-b", "hotfix")
    git.runGitCommand("commit", "-m", "hot fix for issue", "--allow-empty")
    git.runGitCommand("tag", "-a", "1.0.0-hotfix", "-m", "1.0.0-hotfix")
    val commitId = git.currentHeadFullHash
    // switch back to main branch and merge hotfix branch into main branch
    git.runGitCommand("checkout", master)
    git.runGitCommand("merge", commitId, "--no-ff", "-m", "merge commit")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(
      buildResult.output,
      matchesRegex(MERGE_COMMIT_REGEX),
    )
  }

  @Test
  fun `git describe when annotated tag is present after merge commit`() {
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

    // create repository with a single commit tagged as 1.0.0
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // create a new branch called "hotfix" that has a single commit and is tagged with "1.0.0-hotfix"

    val master = git.currentHeadFullHash.substring(0, 7)
    git.runGitCommand("checkout", "-b", "hotfix")
    git.runGitCommand("commit", "-m", "hot fix for issue", "--allow-empty")
    git.runGitCommand("tag", "-a", "1.0.0-hotfix", "-m", "1.0.0-hotfix")
    val commitId = git.currentHeadFullHash

    // switch back to main branch and merge hotfix branch into main branch
    git.runGitCommand("checkout", master)
    git.runGitCommand("merge", commitId, "--no-ff", "-m", "merge commit")

    // tag merge commit on main branch as 2.0.0
    git.runGitCommand("tag", "-a", "2.0.0", "-m", "2.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersion\n2.0.0\n"))
  }

  @Test
  fun `git describe and dirty when annotated tag is present and dirty content`() {
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
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    dirtyContentFile.writeText("dirty-content")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    // assertThat(buildResult.output, containsString(projectDir.getAbsolutePath())
    assertThat(buildResult.output, containsString(":printVersion\n1.0.0.dirty\n"))
  }
}
