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

import eu.aylett.gradle.plugins.BasePlugin
import eu.aylett.gradle.plugins.dependencies.VersionsLocksExtension.Companion.ALL_DEPENDENCIES
import eu.aylett.gradle.plugins.dependencies.VersionsLocksExtension.Companion.ALL_DEPENDENCIES_CLASSPATH
import eu.aylett.gradle.plugins.dependencies.VersionsLocksExtension.Companion.LOCK_CONSTRAINTS
import eu.aylett.gradle.plugins.dependencies.VersionsLocksExtension.Companion.OTHER_PROJECT_DEPENDENCIES
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.intellij.lang.annotations.Language
import java.nio.file.Files
import javax.inject.Inject

/**
 * A plugin that enables version locking for dependency declarations in a Gradle project.
 * It enforces constraints based on a predefined "versions.lock" file, generates this file
 * when needed, and integrates version locking into the project's dependency resolution process.
 *
 * Key functionalities:
 * - Registers configurations for managing all dependencies and for resolving classpath dependencies.
 * - Configures a write task (`writeVersionsLocks`) to generate a "versions.lock" file, capturing
 *   the resolved versions of dependencies.
 * - Applies constraints defined in the "versions.lock" file to the `lockConstraints` configuration.
 * - Ensures the `api` configuration in Java platform projects extends the `lockConstraints`
 *   configuration when the plugin is applied.
 * - Coordinates dependency locking across multiple projects in a build via the DependencySupplierService.
 */
class DependencyLockPlugin
  @Inject
  constructor(
    private var dependencyHandler: DependencyHandler,
    project: Project,
  ) : Plugin<Project> {
    /**
     * Matches the dependency coordinate at the start of a non-comment, non-empty line in `versions.lock`.
     * The expected form is `group:artifact:version`.
     */
    @Language("RegExp")
    private val lockLinePattern: Regex = Regex("^[^ :]+:[^ :]+:[^ :]+")

    private val allDependenciesConfiguration =
      project.configurations
        .consumable(
          ALL_DEPENDENCIES,
        ).get()
    private val allDependenciesClasspath =
      project.configurations.resolvable(ALL_DEPENDENCIES_CLASSPATH).get()
    private val lockConstraints = project.configurations.dependencyScope(LOCK_CONSTRAINTS).get()
    private val otherProjectDependencies =
      project.configurations.dependencyScope(OTHER_PROJECT_DEPENDENCIES).get()

    /**
     * Applies the dependency lock plugin to the given project.
     *
     * - Registers internal configurations used to collect all dependencies and a resolvable classpath.
     * - Creates the `writeVersionsLocks` task that writes a lock file at the root of the build.
     * - On Java Platform projects, enables the write task and converts the lock file entries to constraints
     *   that are added to the platform's `api` configuration.
     */
    override fun apply(target: Project) {
      target.plugins.apply(BasePlugin::class.java)
      target.extensions.create<VersionsLocksExtension>("versionsLocks", target)

      val dependencySupplier = DependencySupplierService.getDependencySupplierService(target)
      dependencySupplier.get().registerProject(target.isolated)

      target.configurations.configureEach {
        if (!this.hierarchy.contains(allDependenciesConfiguration) && !this.isCanBeResolved) {
          allDependenciesConfiguration.extendsFrom(this)
        }
        if (!this.hierarchy.contains(allDependenciesClasspath) && !this.isCanBeConsumed) {
          allDependenciesClasspath.extendsFrom(this)
        }
      }

      val versionsLockFile =
        target.isolated.rootProject.projectDirectory
          .file("versions.lock")
      val writeVersionsLocks =
        target.tasks.register<WriteVersionsLocksTask>("writeVersionsLocks") {
          enabled = false
          output.set(versionsLockFile)
          dependsOn(allDependenciesClasspath)
        }

      if (versionsLockFile.asFile.exists()) {
        Files.readAllLines(versionsLockFile.asFile.toPath()).forEach {
          if (it.isBlank() || it.startsWith("#")) {
            return@forEach
          }
          val matcher =
            lockLinePattern.find(it)
              ?: throw IllegalStateException("Invalid lock file dependencies reading $it")

          val dependencyNotation = matcher.value
          lockConstraints.dependencyConstraints.add(
            dependencyHandler.constraints.create(
              dependencyNotation,
            ),
          )
        }
      }

      target.plugins.withType<JavaPlatformPlugin> {
        writeVersionsLocks.get().enabled = true

        dependencySupplier.get().knownProjects.configureEach {
          if (this.path != target.path) {
            otherProjectDependencies.dependencies.add(
              dependencyHandler.project(
                mapOf(
                  "path" to this.path,
                  "configuration" to ALL_DEPENDENCIES,
                ),
              ),
            )
          }
        }

        target.configurations.named("api").configure {
          extendsFrom(lockConstraints)
        }
      }
    }
  }
