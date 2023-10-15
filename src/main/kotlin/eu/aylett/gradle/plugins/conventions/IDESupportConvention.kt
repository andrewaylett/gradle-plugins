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
