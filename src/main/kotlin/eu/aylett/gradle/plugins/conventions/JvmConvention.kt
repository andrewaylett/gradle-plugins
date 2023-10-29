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

import eu.aylett.gradle.extensions.BaseExtension
import eu.aylett.gradle.extensions.JvmConventionExtension
import eu.aylett.gradle.plugins.BasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

/**
 * Configure Java
 *
 * This sets up the correct toolchain, and applies conventions for consistency. You might also
 * want to apply the `java-library` or `java-application` plugins, if they are
 * appropriate for your project.
 *
 * This plugin is configured inside the `aylett` block. You may override the toolchain
 * version from the default of 17:
 *
 * ```
 * aylett {
 *   jvm {
 *     jvmVersion.set(17)
 *   }
 * }
 * ```
 *
 * Apply this plugin to your project in the plugins block:
 *
 * ```
 * plugins {
 *   id("eu.aylett.conventions.jvm")
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
class JvmConvention : Plugin<Project> {
  override fun apply(target: Project) {
    target.run {
      pluginManager.apply(BasePlugin::class.java)

      val conventionExtension =
        extensions.getByType(BaseExtension::class).extensions.create(
          "jvm",
          JvmConventionExtension::class.java,
        )

      conventionExtension.jvmVersion.convention(17)

      plugins.withType<JavaBasePlugin>().configureEach {
        target.extensions.getByType<JavaPluginExtension>().toolchain {
          languageVersion.set(conventionExtension.jvmVersion.map(JavaLanguageVersion::of))
        }
      }
    }
  }
}
