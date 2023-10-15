package eu.aylett.gradle.plugins.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class Conventions : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.run {
      apply(BomAlignmentConvention::class)
      apply(IDESupportConvention::class)
      apply(JvmConvention::class)
    }
  }
}
