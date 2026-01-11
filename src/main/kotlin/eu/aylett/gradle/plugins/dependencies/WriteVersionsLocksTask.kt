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

import eu.aylett.gradle.plugins.dependencies.VersionsLocksExtension.Companion.ALL_DEPENDENCIES_CLASSPATH
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ComponentSelectionCause
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.stream.Collectors
import javax.inject.Inject

/**
 * Task that writes a dependency versions lock file for the current build.
 *
 * This task resolves the full dependency graph from the `allDependenciesClasspath` configuration and
 * writes a `versions.lock` file with one line per external module in the form
 * `group:artifact:version (…resolution reasons…)`.
 */
@CacheableTask
abstract class WriteVersionsLocksTask : DefaultTask() {
  /** The destination file for the generated lock. Typically `<root>/versions.lock`. */
  @get:OutputFile
  abstract val output: RegularFileProperty

  /**
   * Project configurations used by the task. The task reads dependencies from
   * the `allDependenciesClasspath` configuration.
   */
  @get:Inject
  abstract val configurations: ConfigurationContainer

  /** Resolves dependencies and writes the lock file to [output]. */
  @TaskAction
  fun writeLockFile() {
    val allDependenciesClasspath = configurations.getByName(ALL_DEPENDENCIES_CLASSPATH)
    allDependenciesClasspath.resolve()
    val allDependencies = allDependenciesClasspath.incoming.resolutionResult.allDependencies
    val depMap =
      allDependencies
        .stream()
        .mapMulti { it, consumer ->
          if (it is ResolvedDependencyResult) {
            val selected = it.selected
            val moduleVersion = selected.moduleVersion
            if (selected.id !is ProjectComponentIdentifier && moduleVersion != null &&
              moduleVersion.name.isNotEmpty() &&
              moduleVersion.group.isNotEmpty() &&
              moduleVersion.version.isNotEmpty()
            ) {
              consumer.accept(Pair(selected, moduleVersion))
            }
          }
        }.collect(
          Collectors.toMap({ Pair(it.second.group, it.second.name) }, { it }, { a, _ -> a }),
        )
    output.get().asFile.bufferedWriter().use { writer ->
      writer.appendLine(
        "# Run ./gradlew writeVersionsLocks to regenerate this file. Blank lines are to minimize merge conflicts.",
      )
      depMap.values
        .stream()
        .sorted(compareBy({ it.second.group }, { it.second.name }))
        .map {
          "\n${it.second.group}:${it.second.name}:${it.second.version} (${
            it.first.selectionReason.descriptions.joinToString(
              ", ",
            ) { it.description }
          })"
        }.forEachOrdered { writer.appendLine(it) }
    }
  }
}
