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
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test
import kotlin.io.path.appendText
import kotlin.io.path.writeText

class GitVersionPluginConfigurationCacheKotlinTests : GitVersionPluginTests(kotlin = true) {
  @Test
  fun `caches and prints a version that is explicitly set`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id("eu.aylett.plugins.version")
      }
      version = "2.0.0"
      """.trimIndent(),
    )
    propertiesFile.writeText(
      """
      org.gradle.parallel=true
      org.gradle.caching=true
      org.gradle.configuration-cache=true
      """.trimIndent(),
    )

    git(projectDir) {
      init(projectDir.toString())
    }

    // when:
    val buildResult = with("printVersion").build()
    val buildResult2 = with("printVersion").build()

    // then:
    assertThat(
      buildResult.output.split('\n'),
      hasItems("2.0.0", "Configuration cache entry stored."),
    )
    assertThat(
      buildResult2.output.split('\n'),
      hasItems("2.0.0", "Configuration cache entry reused."),
    )
  }

  @Test
  fun `caches and prints a version from a tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id("eu.aylett.conventions.jvm")
        id("eu.aylett.plugins.version")
        `java-library`
        id("com.gradle.plugin-publish") version "1.3.0"
      }
      version = aylett.versions.gitVersion()

      val checkPublishVersion by tasks.registering {
        doNotTrackState("Either does nothing or fails the build")
        doFirst {
          val versionDetails = aylett.versions.versionDetails()
          if (!versionDetails.isCleanTag) {
            logger.error("Version details is {}", versionDetails)
            throw IllegalStateException(
              "Can't publish a plugin with a version that's not a clean tag",
            )
          }
        }
      }
      tasks.named("publishPlugins").configure {
        dependsOn(checkPublishVersion)
      }

      aylett {
        jvm {
          jvmVersion.set(17)
        }
      }

      """.trimIndent(),
    )
    propertiesFile.writeText(
      """
      org.gradle.parallel=true
      org.gradle.caching=true
      org.gradle.configuration-cache=true
      """.trimIndent(),
    )
    settingsFile.writeText(
      """
      enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")

    git(projectDir) {
      init(projectDir.toString())
      add(".")
      commit("-m", "initial commit")
      tag("-a", "v1.0.0", "-m", "1.0.0")
    }

    // when:
    val buildResult = with("printVersion").build()
    val buildResult2 = with("printVersion").build()

    // then:
    assertThat(
      buildResult.output.split('\n'),
      hasItems("1.0.0", "Configuration cache entry stored."),
    )
    assertThat(
      buildResult2.output.split('\n'),
      hasItems("1.0.0", "Configuration cache entry reused."),
    )
  }
}
