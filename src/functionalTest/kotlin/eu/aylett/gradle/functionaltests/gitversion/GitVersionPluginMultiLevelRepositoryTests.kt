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
import kotlin.io.path.appendText
import kotlin.io.path.writeText

class GitVersionPluginMultiLevelRepositoryTests : GitVersionPluginTests(false, "level1/level2") {
  @Test
  fun `git describe works when git repo is multiple levels up`() {
    // given:
    val rootFolder = temporaryFolder
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    settingsFile.appendText("")
    git(projectDir) {
      init(rootFolder.toFile().absolutePath)
      add(".")
      commit("-m", "initial commit")
      tag("-a", "1.0.0", "-m", "1.0.0")
    }

    // when:
    // will build the project at projectDir
    val buildResult = with("printVersion").build()

    // then:
    assertThat(buildResult.output.split('\n'), containsInRelativeOrder("1.0.0"))
  }
}
