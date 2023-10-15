package eu.aylett.gradle.plugins.conventions

import org.assertj.core.api.Assertions.assertThat
import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class ConventionsTest {
  @Test
  fun `plugin applies`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(Conventions::class)
  }

  @Test
  fun `base plugin gets applied`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(Conventions::class)

    assertThat(project.pluginManager).returns(true) { it.hasPlugin("eu.aylett.plugins.base") }
  }
}
