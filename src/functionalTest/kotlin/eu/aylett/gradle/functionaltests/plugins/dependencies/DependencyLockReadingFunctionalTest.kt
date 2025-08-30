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
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class DependencyLockReadingFunctionalTest : AbstractDependencyLockTest() {
  @Test
  fun `can resolve a dependency within a single project`() {
    buildFile.writeText(
      """
      plugins {
          id 'java'
          id 'eu.aylett.lock-dependencies'
      }

      repositories {
        mavenCentral()
      }

      configurations.implementation.extendsFrom(configurations.lockConstraints)

      dependencies {
        implementation("eu.aylett:gradle-plugins:0.5.2")
      }
      """.trimIndent(),
    )

    lockFile.writeText(
      """
      # Run ./gradlew writeVersionsLocks to regenerate this file. Blank lines are to minimize merge conflicts.

      eu.aylett:gradle-plugins:0.6.1 (requested)
      """.trimIndent(),
    )

    run("dependencies", "--configuration", "compileClasspath")

    assertThat(result.output, containsString("eu.aylett:gradle-plugins:0.5.2 -> 0.6.1"))
    assertThat(result.task(":dependencies")!!.outcome, `is`(TaskOutcome.SUCCESS))
  }

  @Test
  fun `can resolve a dependency within a single project using the extension`() {
    buildFile.writeText(
      """
      plugins {
          id 'java'
          id 'eu.aylett.lock-dependencies'
      }

      repositories {
        mavenCentral()
      }

      versionsLocks.extendAllConfigurations()

      dependencies {
        implementation("eu.aylett:gradle-plugins:0.5.2")
      }
      """.trimIndent(),
    )

    lockFile.writeText(
      """
      # Run ./gradlew writeVersionsLocks to regenerate this file. Blank lines are to minimize merge conflicts.

      eu.aylett:gradle-plugins:0.6.1 (requested)
      """.trimIndent(),
    )

    run("dependencies", "--configuration", "compileClasspath")

    assertThat(result.output, containsString("eu.aylett:gradle-plugins:0.5.2 -> 0.6.1"))
    assertThat(result.task(":dependencies")!!.outcome, `is`(TaskOutcome.SUCCESS))
  }

  @Test
  fun `can resolve a dependency via a platform subproject`() {
    buildFile.writeText(
      """
      plugins {
          id 'java'
          id 'eu.aylett.lock-dependencies'
      }

      repositories {
        mavenCentral()
      }

      dependencies {
        implementation(platform(project(":platform")))
        implementation("eu.aylett:gradle-plugins:0.5.2")
      }
      """.trimIndent(),
    )

    val platform = subproject("platform")

    platform.resolve("build.gradle").writeText(
      """
      plugins {
          id 'java-platform'
          id 'eu.aylett.lock-dependencies'
      }

      repositories {
        mavenCentral()
      }
      """.trimIndent(),
    )

    lockFile.writeText(
      """
      # Run ./gradlew writeVersionsLocks to regenerate this file. Blank lines are to minimize merge conflicts.

      eu.aylett:gradle-plugins:0.6.1 (requested)
      """.trimIndent(),
    )

    run("dependencies", "--configuration", "compileClasspath")

    assertThat(result.output, containsString("eu.aylett:gradle-plugins:0.5.2 -> 0.6.1"))
    assertThat(result.task(":dependencies")!!.outcome, `is`(TaskOutcome.SUCCESS))
  }

  @Test
  fun `can resolve a dependency via a sibling platform subproject`() {
    val buildProject = subproject("build")
    buildProject.resolve("build.gradle").writeText(
      """
      plugins {
          id 'java'
          id 'eu.aylett.lock-dependencies'
      }

      repositories {
        mavenCentral()
      }

      dependencies {
        implementation(platform(project(":platform")))
        implementation("eu.aylett:gradle-plugins:0.5.2")
      }
      """.trimIndent(),
    )

    val platform = subproject("platform")

    platform.resolve("build.gradle").writeText(
      """
      plugins {
          id 'java-platform'
          id 'eu.aylett.lock-dependencies'
      }

      repositories {
        mavenCentral()
      }
      """.trimIndent(),
    )

    lockFile.writeText(
      """
      # Run ./gradlew writeVersionsLocks to regenerate this file. Blank lines are to minimize merge conflicts.

      eu.aylett:gradle-plugins:0.6.1 (requested)
      """.trimIndent(),
    )

    run(":build:dependencies", "--configuration", "compileClasspath")

    assertThat(result.output, containsString("eu.aylett:gradle-plugins:0.5.2 -> 0.6.1"))
    assertThat(result.task(":build:dependencies")!!.outcome, `is`(TaskOutcome.SUCCESS))
  }
}
