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

@file:Suppress("UnstableApiUsage")

package eu.aylett.gradle.plugins.conventions

import eu.aylett.gradle.matchers.hasPlugin
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.Description
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.SelfDescribing
import org.hamcrest.TypeSafeMatcher
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream

class BomAlignmentConventionTest {
  @TempDir
  lateinit var projectDir: File

  @Test
  fun `plugin applies`() {
    val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    project.pluginManager.apply(BomAlignmentConvention::class.java)
    assertThat(
      project.pluginManager,
      hasPlugin("eu.aylett.conventions.bom-alignment"),
    )
  }

  @ParameterizedTest
  @MethodSource("eu.aylett.gradle.plugins.conventions.BomAlignmentConventionTest#specifications")
  fun `plugin sets the version of a dependency`(deps: List<String>) {
    val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    project.pluginManager.apply(JavaPlugin::class.java)
    project.pluginManager.apply(BomAlignmentConvention::class.java)

    project.repositories.mavenCentral()

    val configuration = project.configurations.create("resolveTest")
    val dependencies = deps.map { it: String -> project.dependencyFactory.create(it) }

    configuration.dependencies.apply {
      dependencies.forEach {
        this.add(it)
      }
    }

    configuration.resolve()

    val resolvedVersion = dependencies[0].version
    val resolvedGroup = dependencies[0].group

    val resolved =
      dependencies.filter {
        it.group.equals(resolvedGroup)
      }.map {
        project.dependencyFactory.create(it.group, it.name, resolvedVersion)
      }

    assertThat(
      configuration.resolvedConfiguration.resolvedArtifacts,
      ResolvesToContain(resolved),
    )
  }

  @ParameterizedTest
  @MethodSource("eu.aylett.gradle.plugins.conventions.BomAlignmentConventionTest#specifications")
  fun `no plugin retains the version of a dependency`(deps: List<String>) {
    val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    project.pluginManager.apply(JavaPlugin::class.java)

    project.repositories.mavenCentral()

    val configuration = project.configurations.create("resolveTest")
    val dependencies = deps.map { it: String -> project.dependencyFactory.create(it) }

    configuration.dependencies.apply {
      dependencies.forEach {
        this.add(it)
      }
    }

    configuration.resolve()

    val resolvedGroup = dependencies[0].group
    val resolved =
      dependencies.filter {
        it.group.equals(resolvedGroup)
      }
    assertThat(
      configuration.resolvedConfiguration.resolvedArtifacts,
      ResolvesToContain(resolved),
    )
  }

  companion object {
    @JvmStatic
    fun specifications(): Stream<List<String>> =
      Stream.of(
        listOf(
          "io.dropwizard:dropwizard-util:4.0.2",
          "io.dropwizard:dropwizard-core:4.0.0",
          "com.google.code.findbugs:jsr305:3.0.2",
        ),
        listOf(
          "io.dropwizard.metrics:metrics-core:4.2.20",
          "io.dropwizard.metrics:metrics-jvm:4.2.2",
        ),
        listOf("org.mockito:mockito-core:5.6.0", "org.mockito:mockito-junit-jupiter:5.5.0"),
      )
  }
}

class ResolvesToContain(private val dependencies: List<ExternalModuleDependency>) :
  TypeSafeMatcher<Set<ResolvedArtifact>>() {
  override fun describeTo(description: Description) {
    val selfDescribing =
      dependencies.map {
        SelfDescribing { d -> d.appendValueList("<", ":", ">", it.group, it.name, it.version) }
      }
    description.appendText("A configuration that includes dependencies on ")
    description.appendList("[", ", ", "]", selfDescribing)
  }

  override fun matchesSafely(item: Set<ResolvedArtifact>): Boolean {
    for (required in dependencies) {
      if (!item.any { required.matchesStrictly(it.moduleVersion.id) }) {
        return false
      }
    }
    return true
  }
}
