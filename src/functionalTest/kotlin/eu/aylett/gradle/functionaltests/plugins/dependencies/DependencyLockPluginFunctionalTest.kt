/*
 * Copyright 2025 Andrew Aylett
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

package eu.aylett.gradle.functionaltests.plugins.dependencies

import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.hamcrest.io.FileMatchers.anExistingFile
import org.junit.jupiter.api.Test

class DependencyLockPluginFunctionalTest : AbstractDependencyLockTest() {
  @Test
  fun `can successfully apply the plugin and run its (skipped) task`() {
    buildFile.writeText(
      """
      plugins {
          id 'eu.aylett.lock-dependencies'
      }

      """.trimIndent(),
    )

    run("writeVersionsLocks")

    assertThat(result.task(":writeVersionsLocks")!!.outcome, `is`(TaskOutcome.SKIPPED))
    assertThat(lockFile, not(anExistingFile()))
  }
}
