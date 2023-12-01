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
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.Test
import java.util.regex.Pattern
import kotlin.io.path.appendText
import kotlin.io.path.writeText

private val DETACHED_HEAD_MODE_REGEX: Pattern =
  Pattern.compile(
    "[a-z0-9]{10}",
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
    }
    dirtyContentFile.writeText("dirty-content")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("false"))
  }

  @Test
  fun `version details when detached HEAD mode`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion()
      task printVersionDetails {
        doLast {
          println versionDetails().lastTag
          println versionDetails().commitDistance
          println versionDetails().gitHash
          println versionDetails().branchName
        }
      }

      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      val commitId = currentHeadFullHash
      tag("-a", "1.0.0", "-m", "1.0.0")
      commit("-m", "commit 2", "--allow-empty")
      checkout(commitId)
    }

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder(
        equalTo("1.0.0"),
        equalTo("0"),
        matchesRegex(DETACHED_HEAD_MODE_REGEX),
        equalTo(""),
      ),
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "my-product@1.0.0", "-m", "my-product@1.0.0")
      commit("-m", "commit 2", "--allow-empty")
      tag("-a", "1.0.0", "-m", "1.0.0")
    }

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("1.0.0"))
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "v1.0.0", "-m", "1.0.0")
      dirtyContentFile.writeText("dirty-content")
      add(".")
      commit("-m", "add some stuff")
      val commitSha = currentHeadFullHash

      // when:
      val buildResult = with("printVersion").build()

      // then:
      assertThat(
        buildResult.output.split('\n'),
        containsInRelativeOrder("1.0.0-1-g${commitSha.substring(0, 7)}"),
      )
    }
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("v1.0.0")
      dirtyContentFile.writeText("dirty-content")
      add(".")
      commit("-m", "add some stuff")
      val commitSha = currentHeadFullHash

      // when:
      val buildResult = with("printVersion").build()

      // then:
      assertThat(
        buildResult.output.split('\n'),
        containsInRelativeOrder("1.0.0-1-g${commitSha.substring(0, 7)}"),
      )
    }
  }
}
