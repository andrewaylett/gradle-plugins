package eu.aylett.gradle.plugins.conventions

import org.gradle.kotlin.dsl.apply
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class BomAlignmentConventionTest {
  @Test
  fun `plugin applies`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(BomAlignmentConvention::class)
  }
}
