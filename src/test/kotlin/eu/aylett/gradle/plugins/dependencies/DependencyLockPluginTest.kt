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

package eu.aylett.gradle.plugins.dependencies

import eu.aylett.gradle.matchers.hasPlugin
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test

class DependencyLockPluginTest {
  @Test
  fun `can apply plugin`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(DependencyLockPlugin::class.java)

    assertThat(project, hasPlugin("eu.aylett.lock-dependencies"))
  }

  @Test
  fun `registers writeVersionsLocks task`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(DependencyLockPlugin::class.java)

    assertThat(project.tasks.findByName("writeVersionsLocks"), notNullValue())
  }

  @Test
  fun `writeVersionsLocks task is disabled by default`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(DependencyLockPlugin::class.java)

    val task = project.tasks.findByName("writeVersionsLocks")
    assertThat(task?.enabled, equalTo(false))
  }

  @Test
  fun `can apply plugin to multiple projects`() {
    val rootProject = ProjectBuilder.builder().build()
    val childProject = ProjectBuilder.builder().withParent(rootProject).build()

    rootProject.pluginManager.apply(DependencyLockPlugin::class.java)
    childProject.pluginManager.apply(DependencyLockPlugin::class.java)
    childProject.pluginManager.apply(JavaPlatformPlugin::class.java)

    assertThat(rootProject, hasPlugin("eu.aylett.lock-dependencies"))
    assertThat(childProject, hasPlugin("eu.aylett.lock-dependencies"))

    val child = childProject.tasks.getByName("writeVersionsLocks")
    assertThat(child.enabled, equalTo(true))
    val parent = rootProject.tasks.getByName("writeVersionsLocks")
    assertThat(parent.enabled, equalTo(false))
  }

  @Test
  fun `sets the api configuration to depend on lock file in platform projects`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(DependencyLockPlugin::class.java)
    project.pluginManager.apply(JavaPlatformPlugin::class.java)

    val apiConfig = project.configurations.getByName("api")
    val lockFileConfig = project.configurations.getByName("lockConstraints")

    assertThat(apiConfig.extendsFrom, hasItem(lockFileConfig))
  }
}
