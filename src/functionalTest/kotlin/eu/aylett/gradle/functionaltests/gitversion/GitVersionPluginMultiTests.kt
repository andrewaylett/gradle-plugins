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
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import kotlin.io.path.appendText
import kotlin.io.path.writeText

class GitVersionPluginMultiTests : GitVersionPluginTests() {
  @Test
  fun `test subproject version`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      subprojects {
        apply plugin : "eu.aylett.plugins.version"
        version gitVersion ()
      }
      """.trimIndent(),
    )

    settingsFile.appendText("include \"sub\"")

    gitIgnoreFile.appendText("build\n")
    gitIgnoreFile.appendText("sub\n")

    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    val subDir = Files.createDirectory(temporaryFolder.resolve("sub"))
    val subGit = Git(subDir, true)
    subGit.runGitCommand("init", subDir.toString())
    val subDirty = subDir.resolve("subDirty")
    Files.createFile(subDirty)
    subGit.runGitCommand("add", ".")
    subGit.runGitCommand("commit", "-m", "initial commit sub")
    subGit.runGitCommand("tag", "-a", "8.8.8", "-m", "8.8.8")

    // when:
    val buildResult = with("printVersion", ":sub:printVersion").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersion\n1.0.0\n"))
    assertThat(buildResult.output, containsString(":sub:printVersion\n8.8.8\n"))
  }

  @Test
  fun `test multiple tags on same commit - annotated tag is chosen`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      subprojects {
        apply plugin : "eu.aylett.plugins.version"
        version gitVersion ()
      }
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "1.0.0")
    git.runGitCommand("tag", "-a", "2.0.0", "-m", "2.0.0")
    git.runGitCommand("tag", "3.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersion\n2.0.0\n"))
  }

  @Test
  fun `test multiple tags on same commit - most recent annotated tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      subprojects {
        apply plugin : "eu.aylett.plugins.version"
        version gitVersion ()
      }
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    val d1 = Instant.now() - Duration.ofSeconds(1)
    val envvar1 = HashMap<String, String>()
    envvar1["GIT_COMMITTER_DATE"] = d1.toString()
    git.runGitCommand(
      envvar1,
      "-c",
      "user.name=\"name\"",
      "-c",
      "user.email=email@example.com",
      "tag",
      "-a",
      "1.0.0",
      "-m",
      "1.0.0",
    )
    val d2 = Instant.now()
    val envvar2 = HashMap<String, String>()
    envvar2["GIT_COMMITTER_DATE"] = d2.toString()
    git.runGitCommand(
      envvar2,
      "-c",
      "user.name=\"name\"",
      "-c",
      "user.email=email@example.com",
      "tag",
      "-a",
      "2.0.0",
      "-m",
      "2.0.0",
    )
    val d3 = Instant.now() - Duration.ofSeconds(1)
    val envvar3 = HashMap<String, String>()
    envvar3["GIT_COMMITTER_DATE"] = d3.toString()
    git.runGitCommand(
      envvar3,
      "-c",
      "user.name=\"name\"",
      "-c",
      "user.email=email@example.com",
      "tag",
      "-a",
      "3.0.0",
      "-m",
      "3.0.0",
    )

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersion\n2.0.0\n"))
  }

  @Test
  fun `test multiple tags on same commit - smaller unannotated tag is chosen`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      subprojects {
        apply plugin : "eu.aylett.plugins.version"
        version gitVersion ()
      }
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "2.0.0")
    git.runGitCommand("tag", "1.0.0")
    git.runGitCommand("tag", "3.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output, containsString(":printVersion\n1.0.0\n"))
  }

  @Test
  fun `test tag set on deep commit`() {
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
    var latestCommit = git.currentHeadFullHash

    val depth = 100
    for (i in 0..99) {
      git.runGitCommand("add", ".")
      git.runGitCommand("commit", "-m", "commit-$i", "--allow-empty")
      latestCommit = git.currentHeadFullHash
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(
      buildResult.output,
      containsString(":printVersion\n1.0.0-$depth-g${latestCommit.substring(0, 7)}\n"),
    )
  }
}
