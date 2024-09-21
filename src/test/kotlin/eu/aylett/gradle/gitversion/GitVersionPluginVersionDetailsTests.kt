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

import org.gradle.kotlin.dsl.invoke
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.Test
import java.util.regex.Pattern
import kotlin.io.path.appendText
import kotlin.io.path.writeText

private val SHA_PATTERN: Pattern =
  Pattern.compile("[a-z0-9]{10}")
private val SHA_FULL_PATTERN: Pattern =
  Pattern.compile("[a-z0-9]{40}")

class GitVersionPluginVersionDetailsTests : GitVersionPluginTests() {
  @Test
  fun `version details on commit with a tag`() {
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
          println versionDetails ().gitHashFull
          println versionDetails ().branchName
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
      tag("-a", "1.0.0", "-m", "1.0.0")
    }

    // then:
    assertThat(project.versionDetails().lastTag, equalTo("1.0.0"))
    assertThat(project.versionDetails().commitDistance, equalTo(0))
    assertThat(project.versionDetails().gitHash, matchesRegex(SHA_PATTERN))
    assertThat(project.versionDetails().gitHashFull, matchesRegex(SHA_FULL_PATTERN))
    assertThat(project.versionDetails().branchName, equalTo("main"))
    assertThat(project.versionDetails().isCleanTag, equalTo(true))
  }

  @Test
  fun `version details when commit distance to tag is gt 0`() {
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
      tag("-a", "1.0.0", "-m", "1.0.0")
      commit("-m", "commit 2", "--allow-empty")
    }

    // then:
    assertThat(project.versionDetails().lastTag, equalTo("1.0.0"))
    assertThat(project.versionDetails().commitDistance, equalTo(1))
    assertThat(project.versionDetails().gitHash, matchesRegex(SHA_PATTERN))
    assertThat(project.versionDetails().gitHashFull, matchesRegex(SHA_FULL_PATTERN))
    assertThat(project.versionDetails().branchName, equalTo("main"))
    assertThat(project.versionDetails().isCleanTag, equalTo(false))
  }
}
