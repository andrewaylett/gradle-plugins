package eu.aylett.gradle.plugins.conventions

import eu.aylett.gradle.rules.BomAlignmentRule
import org.gradle.api.Plugin
import org.gradle.api.Project

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
    target.dependencies.components.all(BomAlignmentRule::class.java)
  }
}
