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

import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class VersionsLocksExtensionTest {
  @Test
  fun getVersionsLockFile() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(DependencyLockPlugin::class.java)

    val extension = project.extensions.getByType(VersionsLocksExtension::class.java)
    val lockFile = extension.versionsLockFile
    assertThat(lockFile.get().asFile.name, equalTo("versions.lock"))
  }

  @Test
  fun extendAllConfigurations() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JavaPlugin::class.java)
    project.pluginManager.apply(DependencyLockPlugin::class.java)

    val extension = project.extensions.getByType(VersionsLocksExtension::class.java)
    extension.extendAllConfigurations()

    assertThat(
      project.configurations.getByName("implementation").hierarchy,
      Matchers.hasItem(project.configurations.getByName("lockConstraints")),
    )
    assertThat(
      project.configurations.getByName("testImplementation").hierarchy,
      Matchers.hasItem(project.configurations.getByName("lockConstraints")),
    )
  }

  @Test
  fun enableLockWriting() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(DependencyLockPlugin::class.java)
    val task = project.tasks.getByName("writeVersionsLocks")

    assertThat(task.enabled, equalTo(false))

    val extension = project.extensions.getByType(VersionsLocksExtension::class.java)
    extension.enableLockWriting()

    assertThat(task.enabled, equalTo(true))
  }
}
