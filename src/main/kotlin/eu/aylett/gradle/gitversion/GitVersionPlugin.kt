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

import eu.aylett.gradle.extensions.BaseExtension
import eu.aylett.gradle.plugins.BasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

@Suppress("unused")
class GitVersionPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(BasePlugin::class)
    val versionCacheService =
      GitVersionCacheService.getSharedGitVersionCacheService(
        project,
      )

    val baseExtension = project.extensions.getByType(BaseExtension::class.java)

    val ext =
      baseExtension.extensions.create(
        "versions",
        GitVersionExtension::class.java,
        versionCacheService,
      )

    project.extensions.extraProperties["gitVersion"] = ext.gitVersion
    project.extensions.extraProperties["versionDetails"] = ext.versionDetails

    val versionProperty = project.objects.property(String::class.java)
    project.afterEvaluate {
      versionProperty.set(project.version.toString())
    }

    project.tasks.register("printVersion") {
      doNotTrackState("Only prints to stdout")
      doLast { println(versionProperty.get()) }
      group = "Versioning"
      description = "Prints the project's configured version to standard out"
    }
  }
}
