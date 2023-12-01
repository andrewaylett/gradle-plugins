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

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInRelativeOrder
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

    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "1.0.0", "-m", "1.0.0")
    }

    val subDir = Files.createDirectory(temporaryFolder.resolve("sub"))
    git(subDir) {
      init(subDir.toString())
      val subDirty = subDir.resolve("subDirty")
      Files.createFile(subDirty)
      add(".")
      commit("-m", "initial commit sub")
      tag("-a", "8.8.8", "-m", "8.8.8")
    }

    // when:
    val buildResult = with("printVersion", ":sub:printVersion").build()

    // then:
    assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder(
        "1.0.0",
        "8.8.8",
      ),
    )
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("1.0.0")
      tag("-a", "2.0.0", "-m", "2.0.0")
      tag("3.0.0")
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("2.0.0"))
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      val d1 = Instant.now() - Duration.ofSeconds(1)
      val envvar1 = HashMap<String, String>()
      envvar1["GIT_COMMITTER_DATE"] = d1.toString()
      runGitCommand(
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
      runGitCommand(
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
      runGitCommand(
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
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("2.0.0"))
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("2.0.0")
      tag("1.0.0")
      tag("3.0.0")
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("1.0.0"))
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
    val depth = 100
    val git =
      git(projectDir) {
        init(projectDir.toString())
        add(".")
        commit("-m", "initial commit")
        tag("-a", "1.0.0", "-m", "1.0.0")

        for (i in 0 until depth) {
          add(".")
          commit("-m", "commit-$i", "--allow-empty")
        }
      }
    val latestCommit = git.currentHeadFullHash

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder("1.0.0-$depth-g${latestCommit.substring(0, 7)}"),
    )
  }
}
