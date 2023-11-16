/*
 * Copyright 2023 Andrew Aylett
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

package eu.aylett.gradle.functionaltests

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AllPluginsFunctionalTest {
  @TempDir
  lateinit var testProjectDir: File
  private lateinit var buildFile: File

  @BeforeEach
  fun setup() {
    buildFile = File(testProjectDir, "build.gradle")
    buildFile.writeText(
      """
      plugins {
          id 'eu.aylett.conventions'
          id 'eu.aylett.plugins.version'
      }

      """.trimIndent(),
    )
  }

  @Test
  fun `can successfully apply the project`() {
    buildFile.appendText(
      """
      aylett {
          jvm {
              jvmVersion.set(17)
          }
      }
      """.trimIndent(),
    )

    GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("check")
      .withPluginClasspath()
      .build()
  }

  @Test
  fun `can successfully apply the project and it has no dependencies`() {
    buildFile.appendText(
      """
      aylett {
          jvm {
              jvmVersion.set(17)
          }
      }
      """.trimIndent(),
    )

    val result =
      GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("dependencies")
        .withPluginClasspath()
        .build()

    assertThat(result.output, containsString("No configurations"))
    assertThat(result.task(":dependencies")?.outcome, `is`(TaskOutcome.SUCCESS))
  }
}
