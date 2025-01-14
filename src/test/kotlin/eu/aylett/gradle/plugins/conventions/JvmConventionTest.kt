/*
 * Copyright 2023-2025 Andrew Aylett
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

package eu.aylett.gradle.plugins.conventions

import eu.aylett.gradle.matchers.hasPlugin
import org.gradle.api.GradleException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.core.AllOf.allOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JvmConventionTest {
  @Test
  fun `plugin applies`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JvmConvention::class.java)
  }

  @Test
  fun `does not apply java or kotlin plugins`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JvmConvention::class.java)

    assertThat(project, allOf(not(hasPlugin("java")), not(hasPlugin("kotlin"))))
  }

  @Test
  fun `Configures java if present`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JvmConvention::class.java)
    project.pluginManager.apply(JavaPlugin::class.java)

    // Trigger project evaluation
    project.getTasksByName("check", false)

    assertThat(project, allOf(hasPlugin("java"), not(hasPlugin("kotlin"))))

    assertThat(
      project.extensions
        .getByType<JavaPluginExtension>()
        .toolchain.languageVersion
        .get(),
      equalTo(JavaLanguageVersion.of(21)),
    )
  }

  companion object {
    @JvmStatic
    @BeforeAll
    fun `ensure global services is initialised`() {
      try {
        ProjectBuilder.builder().build()
      } catch (e: GradleException) {
        assertThat(e.message, equalTo("Could not inject synthetic classes."))
      }
    }
  }
}
