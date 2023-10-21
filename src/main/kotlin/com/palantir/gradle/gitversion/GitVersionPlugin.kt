/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.gradle.gitversion

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class GitVersionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.rootProject.pluginManager.apply(GitVersionRootPlugin::class.java)
    val serviceProvider: Provider<GitVersionCacheService> =
      GitVersionCacheService.Companion.getSharedGitVersionCacheService(project)

    // intentionally not using .getExtension() here for back-compat
    project.extensions.extraProperties["gitVersion"] =
      object : Closure<String?>(this, this) {
        fun doCall(args: Any?): String? {
          return serviceProvider.get().getGitVersion(project.projectDir, args)
        }
      }
    project.extensions.extraProperties["versionDetails"] =
      object : Closure<VersionDetails?>(this, this) {
        fun doCall(args: Any?): VersionDetails? {
          return serviceProvider.get().getVersionDetails(project.projectDir, args)
        }
      }
    val printVersionTask = project.tasks.create("printVersion")
    printVersionTask.doLast { println(project.version) }
    printVersionTask.group = "Versioning"
    printVersionTask.description = "Prints the project's configured version to standard out"
  }
}
