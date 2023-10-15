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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugins.ide.idea.model.IdeaModel

/**
 * Configure IntelliJ IDEA to download sources and Javadoc.
 *
 * This means the IDE will pre-load dependency sources, making them available for reference
 *
 * Apply this plugin to your project in the plugins block:
 *
 * ```
 * plugins {
 *   id("eu.aylett.conventions.ide-support")
 * }
 * ```
 *
 * Or by applying the [Conventions] plugin:
 *
 * ```
 * plugins {
 *   id("eu.aylett.conventions")
 * }
 * ```
 */
class IDESupportConvention : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply("idea")
    target.extensions.getByType(IdeaModel::class).module {
      isDownloadSources = true
      isDownloadJavadoc = true
    }
  }
}
