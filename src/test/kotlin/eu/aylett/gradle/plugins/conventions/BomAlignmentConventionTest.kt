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
import org.gradle.api.GradleException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream

class BomAlignmentConventionTest {
  private val extraDeps =
    listOf(
      "com.hubspot.jinjava:jinjava:2.7.0",
      "com.googlecode.concurrent-trees:concurrent-trees:2.6.1",
    )

  @Test
  fun `plugin applies on its own`() {
    val project = ProjectBuilder.builder().withGradleUserHomeDir(userHomeDir).build()
    project.pluginManager.apply(
      BomAlignmentConvention::class.java,
    )
  }

  @TestFactory
  fun `plugin sets the version of a dependency`(): Stream<DynamicTest> =
    DynamicTest.stream(specifications(), { deps -> deps.modifiedDeps[0] }) { deps ->
      val project = ProjectBuilder.builder().withGradleUserHomeDir(userHomeDir).build()
      project.pluginManager.apply(
        BomAlignmentConvention::class.java,
      )
      project.pluginManager.apply(JavaPlugin::class.java)

      project.repositories.mavenCentral()

      val configuration = project.configurations.create("resolveTest")
      val dependencies =
        (deps.modifiedDeps + deps.unmodifiedDeps + extraDeps).map {
          project.dependencyFactory.create(it)
        }

      configuration.dependencies.apply {
        dependencies.forEach {
          this.add(it)
        }
      }

      configuration.resolve()

      val resolvedVersion = dependencies[0].version

      val resolved =
        dependencies.map {
          project.dependencyFactory.create(
            it.group,
            it.name,
            if (deps.modifiedDeps.any { d -> d.startsWith("${it.group}:${it.name}:") }) {
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

  @TestFactory
  fun `project with no plugin retains the version of a dependency`(): Stream<DynamicTest> =
    DynamicTest.stream(specifications(), { deps -> deps.modifiedDeps[0] }) { deps ->
      val project = ProjectBuilder.builder().withGradleUserHomeDir(userHomeDir).build()
      project.pluginManager.apply(JavaPlugin::class.java)

      project.repositories.mavenCentral()

      val configuration = project.configurations.create("resolveTest")
      val dependencies =
        (deps.modifiedDeps + deps.unmodifiedDeps + extraDeps).map {
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

  data class Spec(
    val modifiedDeps: List<String>,
    val unmodifiedDeps: List<String>,
  )

  @Suppress("ktlint:standard:max-line-length")
  private fun specifications(): Stream<Spec> =
    Stream.of(
      Spec(
        listOf(
          "org.mockito:mockito-core:5.6.0",
          "org.mockito:mockito-junit-jupiter:5.5.0",
        ),
        listOf(),
      ),
      Spec(
        listOf(
          "io.dropwizard.metrics:metrics-core:4.2.20",
          "io.dropwizard.metrics:metrics-jvm:4.2.2",
        ),
        listOf(),
      ),
      Spec(
        listOf(
          "io.dropwizard:dropwizard-util:4.0.2",
          "io.dropwizard:dropwizard-core:4.0.0",
        ),
        listOf(
          "com.google.code.findbugs:jsr305:3.0.2",
        ),
      ),
      Spec(
        listOf(
          "org.glassfish.jersey.core:jersey-common:2.39.1",
          "org.glassfish.jersey.test-framework.providers:jersey-test-framework-provider-inmemory:2.2",
        ),
        listOf(
          "org.glassfish:jakarta.el:3.0.4",
        ),
      ),
      Spec(
        listOf(
          "org.jetbrains.kotlin:kotlin-stdlib:1.9.10",
          "org.jetbrains.kotlin:kotlin-reflect:1.8.0",
        ),
        listOf(
          "org.jetbrains.kotlin:kdoc:0.12.613",
        ),
      ),
      Spec(
        listOf(
          "org.assertj:assertj-core:3.24.2",
          "org.assertj:assertj-guava:3.2.0",
        ),
        listOf(),
      ),
      Spec(
        listOf(
          "com.google.protobuf:protobuf-java:3.24.4",
          "com.google.protobuf:protobuf-java-util:3.23.1",
        ),
        listOf(),
      ),
      Spec(
        listOf(
          "com.google.api.grpc:proto-google-common-protos:2.28.0",
          "com.google.api.grpc:grpc-google-common-protos:2.24.0",
        ),
        listOf(
          "com.google.api.grpc:grpc-google-cloud-trace-v2:2.30.0",
        ),
      ),
      Spec(
        listOf(
          "io.grpc:grpc-api:1.59.0",
          "io.grpc:grpc-netty:1.56.0",
        ),
        listOf(),
      ),
    )

  companion object {
    private val userHomeDir: File =
      Path.of(
        eu.aylett.gradle.generated.PROJECT_DIR,
        "./build/cache",
      ).toFile()

    @JvmStatic
    @BeforeAll
    fun `ensure global services is initialised`() {
      try {
        ProjectBuilder.builder().build()
      } catch (e: GradleException) {
        MatcherAssert.assertThat(e.message, Matchers.equalTo("Could not inject synthetic classes."))
      }
    }
  }
}
