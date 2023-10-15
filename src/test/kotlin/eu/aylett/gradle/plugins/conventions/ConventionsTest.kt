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

package eu.aylett.gradle.plugins.conventions

import org.assertj.core.api.Assertions.assertThat
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class ConventionsTest {
  @Test
  fun `plugin applies`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(Conventions::class)
  }

  @Test
  fun `base plugin gets applied`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(Conventions::class)

    assertThat(project.pluginManager).returns(true) { it.hasPlugin("eu.aylett.plugins.base") }
  }
}
