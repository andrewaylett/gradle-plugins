package eu.aylett.gradle.plugins.conventions

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.junit.jupiter.api.Test

class JvmConventionTest {
  @Test
  fun `plugin applies`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JvmConvention::class)
  }

  @Test
  fun `does not apply java or kotlin plugins`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JvmConvention::class)

    assertThat(project.pluginManager)
      .returns(false) { it.hasPlugin("java") }
      .returns(false) { it.hasPlugin("kotlin") }
  }

  @Test
  fun `Configures java if present`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JvmConvention::class)
    project.pluginManager.apply(JavaPlugin::class)

    // Trigger project evaluation
    project.getTasksByName("check", false)

    assertThat(project.pluginManager)
      .returns(true) { it.hasPlugin("java") }
      .returns(false) { it.hasPlugin("kotlin") }

    assertThat(project.extensions.getByType<JavaPluginExtension>().toolchain.languageVersion)
      .returns(JavaLanguageVersion.of(17), Property<JavaLanguageVersion>::get)
  }

  @Test
  fun `Configures kotlin if present`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JvmConvention::class)
    project.pluginManager.apply(KotlinPlatformJvmPlugin::class)

    // Trigger project evaluation
    project.getTasksByName("check", false)

    assertThat(project.pluginManager)
      .returns(true) { it.hasPlugin("java") }
      .returns(true) { it.hasPlugin("kotlin") }

    assertThat(project.extensions.getByType<JavaPluginExtension>().toolchain.languageVersion)
      .returns(JavaLanguageVersion.of(17), Property<JavaLanguageVersion>::get)

    val kotlinToolchain = project.objects.property<JavaLanguageVersion>()
    project.extensions.getByType<KotlinJvmProjectExtension>().jvmToolchain {
      kotlinToolchain.set(languageVersion)
    }

    assertThat(kotlinToolchain).returns(
      JavaLanguageVersion.of(17),
      Property<JavaLanguageVersion>::get,
    )
  }
}
