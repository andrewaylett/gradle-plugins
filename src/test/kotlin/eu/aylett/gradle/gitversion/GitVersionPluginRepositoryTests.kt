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
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    // then:
    assertThat(
      assertThrows<Exception> { project.gitVersion() },
      Matchers.hasToString(containsString("not a git repository")),
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

    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "1.0.0", "-m", "1.0.0")
    }

    // then:
    assertThat(project.gitVersion(), equalTo("1.0.0"))
  }
}
