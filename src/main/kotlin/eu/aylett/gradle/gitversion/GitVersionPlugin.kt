/*
 * Copyright 2023 Andrew Aylett
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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
package eu.aylett.gradle.gitversion

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.KotlinClosure1

@Suppress("unused")
class GitVersionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.rootProject.pluginManager.apply(GitVersionRootPlugin::class.java)
    val serviceProvider = GitVersionCacheService.getSharedGitVersionCacheService(project)

    val projectDir = project.projectDir.toPath()
    project.extensions.extraProperties["gitVersion"] =
      KotlinClosure1<Any?, String>({
        serviceProvider.get().getGitVersion(projectDir, this)
      })

    project.extensions.extraProperties["versionDetails"] =
      KotlinClosure1<Any?, VersionDetails>({
        serviceProvider.get().getVersionDetails(projectDir, this)
      })

    project.tasks.register("printVersion") {
      doLast { println(project.version) }
      group = "Versioning"
      description = "Prints the project's configured version to standard out"
    }
  }
}
