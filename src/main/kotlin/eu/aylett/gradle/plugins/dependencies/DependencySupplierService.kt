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

import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.project.IsolatedProject
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * A build service used to coordinate dependency collection across all projects in a multi-project build.
 *
 * The service tracks all known projects so that the dependency locking plugin can build a complete
 * view of external dependencies before writing the lock file or applying constraints on a platform project.
 */
abstract class DependencySupplierService : BuildService<BuildServiceParameters.None> {
  /** Set of projects participating in dependency collection. */
  abstract val knownProjects: DomainObjectSet<IsolatedProject>

  /** Registers a project with the service. */
  fun registerProject(project: IsolatedProject) {
    knownProjects.add(project)
  }

  companion object {
    /** Gets or creates the shared [DependencySupplierService] instance for the current build. */
    fun getDependencySupplierService(project: Project): Provider<DependencySupplierService> =
      project.gradle
        .sharedServices
        .registerIfAbsent(
          "DependencySupplierService",
          DependencySupplierService::class.java,
        ) {}
  }
}
