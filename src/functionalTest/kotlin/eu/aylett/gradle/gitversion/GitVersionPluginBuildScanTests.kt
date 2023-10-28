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

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.io.path.appendText
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GitVersionPluginBuildScanTests : GitVersionPluginTests() {
  @Test
  fun `can set build scan custom values when Gradle 6 enterprise plugin 3-2 is applied`() {
    // when:
    settingsFile.writeText(
      """
      plugins {
        id "com.gradle.enterprise" version "3.2"
      }
      """.trimIndent() + '\n' + settingsFile.readText(),
    )

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

    // then:
    assertThat(with("printVersion").build(), notNullValue())
  }

  @Test
  fun `can set build scan custom values when Gradle 7 build scan plugin is applied`() {
    // when:
    settingsFile.writeText(
      """
      plugins {
        id "com.gradle.enterprise" version "3.2"
      }
      """.trimIndent() + '\n' + settingsFile.readText(),
    )

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

    // then:
    assertThat(with(Optional.of("7.4.2"), "printVersion").build(), notNullValue())
  }

  @Test
  fun `can set build scan custom values when Gradle 6 enterprise plugin 3-1 is applied`() {
    // when:
    settingsFile.writeText(
      """
      plugins {
        id "com.gradle.enterprise" version "3.1"
      }
      """.trimIndent() + '\n' + settingsFile.readText(),
    )

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

    // then:
    assertThat(with("printVersion").build(), notNullValue())
  }
}
