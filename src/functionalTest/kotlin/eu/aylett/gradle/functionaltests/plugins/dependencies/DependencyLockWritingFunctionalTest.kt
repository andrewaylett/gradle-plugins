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
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.startsWith
import org.hamcrest.io.FileMatchers.anExistingFile
import org.junit.jupiter.api.Test

class DependencyLockWritingFunctionalTest : AbstractDependencyLockTest() {
  @Test
  fun `can successfully apply the plugin and run its (not skipped) task`() {
    buildFile.writeText(
      """
      plugins {
          id 'eu.aylett.lock-dependencies'
      }

      tasks.named("writeVersionsLocks") {
        enabled = true
      }
      """.trimIndent(),
    )

    run("writeVersionsLocks")

    assertThat(result.task(":writeVersionsLocks")!!.outcome, `is`(TaskOutcome.SUCCESS))
    assertThat(lockFile, anExistingFile())
    assertThat(lockFileLines.first(), startsWith("#"))
    assertThat(lockEntries, empty())
  }

  @Test
  fun `use the extension to enable lock file writing`() {
    buildFile.writeText(
      """
      plugins {
          id 'eu.aylett.lock-dependencies'
      }

      versionsLocks.enableLockWriting()
      """.trimIndent(),
    )

    run("writeVersionsLocks")

    assertThat(result.task(":writeVersionsLocks")!!.outcome, `is`(TaskOutcome.SUCCESS))
    assertThat(lockFile, anExistingFile())
    assertThat(lockFileLines.first(), startsWith("#"))
    assertThat(lockEntries, empty())
  }

  @Test
  fun `can successfully write a lockfile with a dependency that has no transitive dependencies`() {
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
        implementation("org.springframework:spring-jcl:5.3.20")
      }

      tasks.named("writeVersionsLocks") {
        enabled = true
      }
      """.trimIndent(),
    )

    run("writeVersionsLocks")

    assertThat(result.task(":writeVersionsLocks")!!.outcome, `is`(TaskOutcome.SUCCESS))
    assertThat(lockFile, anExistingFile())
    assertThat(lockFileLines.first(), startsWith("#"))
    assertThat(lockEntries, hasSize(1))
    assertThat(lockEntries.first(), startsWith("org.springframework:spring-jcl:5.3.20"))
  }

  @Test
  fun `can write a lockfile with a dependency that has some transitive dependencies`() {
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
        implementation("eu.aylett:gradle-plugins:0.5.2")
      }

      tasks.named("writeVersionsLocks") {
        enabled = true
      }
      """.trimIndent(),
    )

    run("writeVersionsLocks")

    assertThat(result.task(":writeVersionsLocks")!!.outcome, `is`(TaskOutcome.SUCCESS))
    assertThat(lockFile, anExistingFile())
    assertThat(lockFileLines.first(), startsWith("#"))

    val expected =
      listOf(
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.errorprone:error_prone_annotations:2.36.0",
        "com.google.guava:failureaccess:1.0.2",
        "com.google.guava:guava:33.4.0-jre",
        "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
        "com.google.j2objc:j2objc-annotations:3.0.0",
        "eu.aylett:gradle-plugins:0.5.2",
        "org.checkerframework:checker-qual:3.43.0",
        "org.jetbrains.kotlin:kotlin-bom:2.0.20",
      )
    assertThat(lockEntries, contains(expected.map { startsWith(it) }))
  }

  @Test
  fun `can write dependencies via a platform subproject`() {
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

    run("writeVersionsLocks")

    assertThat(result.task(":platform:writeVersionsLocks")!!.outcome, `is`(TaskOutcome.SUCCESS))
    assertThat(lockFile, anExistingFile())
    assertThat(lockFileLines.first(), startsWith("#"))

    val expected =
      listOf(
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.errorprone:error_prone_annotations:2.36.0",
        "com.google.guava:failureaccess:1.0.2",
        "com.google.guava:guava:33.4.0-jre",
        "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
        "com.google.j2objc:j2objc-annotations:3.0.0",
        "eu.aylett:gradle-plugins:0.5.2",
        "org.checkerframework:checker-qual:3.43.0",
        "org.jetbrains.kotlin:kotlin-bom:2.0.20",
      )
    assertThat(lockEntries, contains(expected.map { startsWith(it) }))
  }

  @Test
  fun `can update dependencies via a platform subproject`() {
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

    platform.resolve("versions.lock").writeText(
      """
      # Run ./gradlew writeVersionsLocks to regenerate this file. Blank lines are to minimize merge conflicts.

      eu.aylett:gradle-plugins:0.4.0 (requested)
      """.trimIndent(),
    )

    run("writeVersionsLocks")

    assertThat(result.task(":platform:writeVersionsLocks")!!.outcome, `is`(TaskOutcome.SUCCESS))
    assertThat(lockFile, anExistingFile())
    assertThat(lockFileLines.first(), startsWith("#"))

    val expected =
      listOf(
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.errorprone:error_prone_annotations:2.36.0",
        "com.google.guava:failureaccess:1.0.2",
        "com.google.guava:guava:33.4.0-jre",
        "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
        "com.google.j2objc:j2objc-annotations:3.0.0",
        "eu.aylett:gradle-plugins:0.5.2",
        "org.checkerframework:checker-qual:3.43.0",
        "org.jetbrains.kotlin:kotlin-bom:2.0.20",
      )
    assertThat(lockEntries, contains(expected.map { startsWith(it) }))
  }

  @Test
  fun `can update dependencies from a subproject via a platform subproject`() {
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

      eu.aylett:gradle-plugins:0.4.0 (requested)
      """.trimIndent(),
    )

    run("writeVersionsLocks")

    assertThat(result.task(":platform:writeVersionsLocks")!!.outcome, `is`(TaskOutcome.SUCCESS))
    assertThat(lockFile, anExistingFile())
    assertThat(lockFileLines.first(), startsWith("#"))

    val expected =
      listOf(
        "com.google.code.findbugs:jsr305:3.0.2",
        "com.google.errorprone:error_prone_annotations:2.36.0",
        "com.google.guava:failureaccess:1.0.2",
        "com.google.guava:guava:33.4.0-jre",
        "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava",
        "com.google.j2objc:j2objc-annotations:3.0.0",
        "eu.aylett:gradle-plugins:0.5.2",
        "org.checkerframework:checker-qual:3.43.0",
        "org.jetbrains.kotlin:kotlin-bom:2.0.20",
      )
    assertThat(lockEntries, contains(expected.map { startsWith(it) }))
  }
}
