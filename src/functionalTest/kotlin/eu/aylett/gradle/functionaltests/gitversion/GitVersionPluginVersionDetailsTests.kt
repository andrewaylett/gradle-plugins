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

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder(
        equalTo("1.0.0"),
        equalTo("0"),
        matchesRegex(SHA_PATTERN),
        matchesRegex(SHA_FULL_PATTERN),
        equalTo("main"),
        equalTo("true"),
      ),
    )
  }

  @Test
  fun `version details can be accessed using extra properties method`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version = gitVersion()
      task printVersionDetails {
        doLast {
          println project . getExtensions ().getExtraProperties().get("versionDetails")().lastTag
          println project . getExtensions ().getExtraProperties().get("gitVersion")()
        }
      }
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git =
      git(projectDir) {
        init(projectDir.toString())
        add(".")
        commit("-m", "initial commit")
      }
    val sha = git.currentHeadFullHash.subSequence(0, 7)

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder(
        sha,
        sha,
      ),
    )
  }
}
