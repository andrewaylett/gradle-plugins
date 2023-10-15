package eu.aylett.gradle.plugins

import eu.aylett.gradle.extensions.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class BasePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.extensions.create("aylett", BaseExtension::class.java)
  }
}
