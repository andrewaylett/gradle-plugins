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

import eu.aylett.gradle.matchers.ResolvesToContain
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream

class BomAlignmentConventionTest {
  private val extraDeps =
    listOf(
      "com.hubspot.jinjava:jinjava:2.7.0",
      "com.googlecode.concurrent-trees:concurrent-trees:2.6.1",
    )

  @ParameterizedTest
  @MethodSource("eu.aylett.gradle.plugins.conventions.BomAlignmentConventionTest#specifications")
  fun `plugin sets the version of a dependency`(deps: List<String>) {
    val project = ProjectBuilder.builder().withGradleUserHomeDir(userHomeDir).build()
    project.pluginManager.apply(JavaPlugin::class.java)
    project.pluginManager.apply(
      BomAlignmentConvention::class.java,
    )

    project.repositories.mavenCentral()

    val realDeps =
      if (deps.first().contains(':')) {
        deps
      } else {
        deps.drop(1)
      }

    // Notice there's a blank line between dependencies that match the group and are part of the BOM
    // and dependencies that match the group but need to retain their original version.
    val mapDependenciesUpTo = realDeps.indexOf("")

    val configuration = project.configurations.create("resolveTest")
    val dependencies =
      (realDeps + extraDeps).filter(String::isNotBlank).map {
          it: String ->
        project.dependencyFactory.create(it)
      }

    configuration.dependencies.apply {
      dependencies.forEach {
        this.add(it)
      }
    }

    configuration.resolve()

    val resolvedVersion = dependencies[0].version
    val resolvedGroup: String =
      if (deps.first().contains(':')) {
        (dependencies[0] as ModuleVersionSelector).group
      } else {
        deps.first()
      }

    val resolved =
      dependencies.mapIndexed { i, it: ModuleVersionSelector ->
        project.dependencyFactory.create(
          it.group,
          it.name,
          if (it.group.startsWith(resolvedGroup) &&
            (mapDependenciesUpTo == -1 || i < mapDependenciesUpTo)
          ) {
            resolvedVersion
          } else {
            it.version
          },
        )
      }

    MatcherAssert.assertThat(
      configuration.resolvedConfiguration.resolvedArtifacts,
      ResolvesToContain(resolved),
    )
  }

  @ParameterizedTest
  @MethodSource("eu.aylett.gradle.plugins.conventions.BomAlignmentConventionTest#specifications")
  fun `project with no plugin retains the version of a dependency`(deps: List<String>) {
    val project = ProjectBuilder.builder().withGradleUserHomeDir(userHomeDir).build()
    project.pluginManager.apply(JavaPlugin::class.java)

    project.repositories.mavenCentral()

    val realDeps =
      if (deps.first().contains(':')) {
        deps
      } else {
        deps.drop(1)
      }

    val configuration = project.configurations.create("resolveTest")
    val dependencies =
      (realDeps + extraDeps).filter(String::isNotBlank).map {
          it: String ->
        project.dependencyFactory.create(it)
      }

    configuration.dependencies.apply {
      dependencies.forEach {
        this.add(it)
      }
    }

    configuration.resolve()

    MatcherAssert.assertThat(
      configuration.resolvedConfiguration.resolvedArtifacts,
      ResolvesToContain(dependencies),
    )
  }

  companion object {
    @JvmStatic
    @Suppress("ktlint:standard:max-line-length")
    fun specifications(): Stream<List<String>> =
      Stream.of(
        listOf(
          "org.mockito:mockito-core:5.6.0",
          "org.mockito:mockito-junit-jupiter:5.5.0",
        ),
        listOf(
          "io.dropwizard.metrics:metrics-core:4.2.20",
          "io.dropwizard.metrics:metrics-jvm:4.2.2",
        ),
        listOf(
          "io.dropwizard:dropwizard-util:4.0.2",
          "io.dropwizard:dropwizard-core:4.0.0",
          "com.google.code.findbugs:jsr305:3.0.2",
        ),
        listOf(
          // Explicit prefix without a ':'
          "org.glassfish.jersey",
          "org.glassfish.jersey.core:jersey-common:2.39.1",
          "org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-inmemory:2.2",
          "org.glassfish:jakarta.el:3.0.4",
        ),
        listOf(
          "org.jetbrains.kotlin:kotlin-stdlib:1.9.10",
          "org.jetbrains.kotlin:kotlin-reflect:1.8.0",
          // We won't map dependency versions after an empty string
          "",
          "org.jetbrains.kotlin:kdoc:0.12.613",
        ),
        listOf(
          "org.assertj:assertj-core:3.24.2",
          "org.assertj:assertj-guava:3.2.0",
        ),
        listOf(
          "com.google.protobuf:protobuf-java:3.24.4",
          "com.google.protobuf:protobuf-java-util:3.23.1",
        ),
      )

    private val userHomeDir: File =
      Path.of(
        eu.aylett.gradle.generated.PROJECT_DIR,
        "./build/cache",
      ).toFile()

    @JvmStatic
    @BeforeAll
    fun `set up a project`() {
      // Run this once in isolation, to avoid races between tests setting up static metadata :(
      val project =
        ProjectBuilder.builder().withGradleUserHomeDir(userHomeDir)
          .build()
      project.pluginManager.apply(JavaPlugin::class.java)
      project.pluginManager.apply(
        BomAlignmentConvention::class.java,
      )
    }
  }
}
