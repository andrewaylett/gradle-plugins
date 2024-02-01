/*
 * Copyright 2023-2024 Andrew Aylett
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

import eu.aylett.gradle.rules.BomAlignmentRule
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin

/**
 * Add [BomAlignmentRule] to your project dependency lookup.
 *
 *
 * This helps ensure you don't have conflicting versions of libraries in your class path, when
 * the libraries don't publish the data Gradle requires to avoid it automatically.
 *
 *
 * Apply this plugin to your project in the plugins block:
 *
 * <pre>`plugins {
 * id("eu.aylett.conventions.bom-alignment")
 * }
 *
`</pre> *
 *
 * Or by applying the [Conventions] plugin:
 *
 * <pre>`plugins {
 * id("eu.aylett.conventions")
 * }
 *
`</pre> *
 */
class BomAlignmentConvention : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(JavaBasePlugin::class.java)
    target.dependencies.components.all(BomAlignmentRule::class.java)
  }
}
