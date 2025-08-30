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

@file:Suppress("UnstableApiUsage")

package eu.aylett.gradle.plugins.dependencies

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider

abstract class VersionsLocksExtension(
  private var project: Project,
) : ExtensionAware {
  val versionsLockFile: Provider<RegularFile> =
    project.provider {
      project.isolated.projectDirectory.file("versions.lock")
    }

  fun extendAllConfigurations() {
    val lockConstraints = project.configurations.getByName(LOCK_CONSTRAINTS)

    project.configurations.configureEach {
      if (!this.hierarchy.contains(lockConstraints)) {
        if (this.isCanBeDeclared) {
          this.extendsFrom(lockConstraints)
        }
      }
    }
  }

  fun enableLockWriting() {
    project.tasks.named("writeVersionsLocks").configure {
      this.enabled = true
    }
  }

  companion object {
    const val ALL_DEPENDENCIES: String = "allDependencies"
    const val ALL_DEPENDENCIES_CLASSPATH: String = "allDependenciesClasspath"
    const val LOCK_CONSTRAINTS: String = "lockConstraints"
    const val OTHER_PROJECT_DEPENDENCIES: String = "otherProjectDependencies"
  }
}
