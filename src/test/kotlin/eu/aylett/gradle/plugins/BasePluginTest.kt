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

package eu.aylett.gradle.plugins

import eu.aylett.gradle.matchers.hasPlugin
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BasePluginTest {
  @Test
  fun `can apply plugin`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(BasePlugin::class.java)

    assertThat(project.pluginManager, hasPlugin("eu.aylett.plugins.base"))
  }

  companion object {
    @JvmStatic
    @BeforeAll
    fun `ensure global services is initialised`() {
      try {
        ProjectBuilder.builder().build()
      } catch (e: GradleException) {
        assertThat(e.message, Matchers.equalTo("Could not inject synthetic classes."))
      }
    }
  }
}
