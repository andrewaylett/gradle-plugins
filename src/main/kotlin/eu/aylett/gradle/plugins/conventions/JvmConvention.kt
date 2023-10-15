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
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin

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

      plugins.withType<KotlinPlatformJvmPlugin>().configureEach {
        target.extensions.getByType<KotlinJvmProjectExtension>().jvmToolchain {
          languageVersion.set(conventionExtension.jvmVersion.map(JavaLanguageVersion::of))
        }
      }
    }
  }
}
