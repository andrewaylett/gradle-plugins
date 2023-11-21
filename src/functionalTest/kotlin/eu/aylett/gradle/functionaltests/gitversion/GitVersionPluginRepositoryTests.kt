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
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.io.path.appendText
import kotlin.io.path.writeText

class GitVersionPluginRepositoryTests : GitVersionPluginTests() {
  @Test
  fun `exception when project root does not have a git repo`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )

    // when:
    val buildResult = with("printVersion").buildAndFail()

    // then:
    assertThat(
      buildResult.output,
      containsString("> Cannot find '.git' directory"),
    )
  }

  @Test
  fun `git version can be applied on sub modules`() {
    // given:
    val subModuleDir =
      Files.createDirectories(
        projectDir
          .resolve("submodule"),
      ).toFile()
    val subModuleBuildFile = File(subModuleDir, "build.gradle")
    subModuleBuildFile.createNewFile()
    subModuleBuildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )

    settingsFile.appendText(
      """
      include "submodule"
      """.trimIndent(),
    )

    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), Matchers.containsInRelativeOrder("1.0.0"))
  }
}
