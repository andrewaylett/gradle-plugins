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
import org.junit.jupiter.api.Test
import kotlin.io.path.appendText
import kotlin.io.path.writeText

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

    git(projectDir) {
      init(projectDir.toString())
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("unspecified"))
  }

  @Test
  fun `prints a version that is explicitly set`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version "2.0.0"
      """.trimIndent(),
    )

    git(projectDir) {
      init(projectDir.toString())
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("2.0.0"))
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "1.0.0", "-m", "1.0.0")
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("1.0.0"))
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("1.0.0")
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("1.0.0"))
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "1.0.0", "-m", "1.0.0")

      // create a new branch called "hotfix" that has a single commit and is tagged with "1.0.0-hotfix"
      val master = currentHeadFullHash.substring(0, 7)
      checkout("-b", "hotfix")
      commit("-m", "hot fix for issue", "--allow-empty")
      tag("-a", "1.0.0-hotfix", "-m", "1.0.0-hotfix")
      val commitId = currentHeadFullHash
      // switch back to main branch and merge hotfix branch into main branch
      checkout(master)
      merge(commitId, "--no-ff", "-m", "merge commit")
      val targetId = currentHeadFullHash

      // when:
      val buildResult = with("printVersion").build()

      // then:
      assertThat(
        buildResult.output.split('\n'),
        containsInRelativeOrder(
          equalTo("1.0.0-1-g${targetId.substring(0..6)}"),
        ),
      )
    }
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "1.0.0", "-m", "1.0.0")

      // create a new branch called "hotfix" that has a single commit and is tagged with "1.0.0-hotfix"

      val main = currentHeadFullHash.substring(0, 7)
      checkout("-b", "hotfix")
      commit("-m", "hot fix for issue", "--allow-empty")
      tag("-a", "1.0.0-hotfix", "-m", "1.0.0-hotfix")
      val commitId = currentHeadFullHash

      // switch back to main branch and merge hotfix branch into main branch
      checkout(main)
      merge(commitId, "--no-ff", "-m", "merge commit")

      // tag merge commit on main branch as 2.0.0
      tag("-a", "2.0.0", "-m", "2.0.0")
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("2.0.0"))
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
    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "1.0.0", "-m", "1.0.0")
    }
    dirtyContentFile.writeText("dirty-content")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    // assertThat(buildResult.output, containsString(projectDir.getAbsolutePath())
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("1.0.0.dirty"))
  }
}
