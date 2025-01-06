/*
 * Copyright 2023-2025 Andrew Aylett
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

import eu.aylett.gradle.generated.InternalDepsVersions
import java.nio.file.Files


plugins {
  java
  `jvm-test-suite`
  `java-test-fixtures`
  id("org.jetbrains.kotlin.jvm")
}

abstract class GenerateTestProjectConstants : DefaultTask() {
  @get:OutputDirectory
  abstract val baseDir: DirectoryProperty

  @get:Input
  abstract val projectDir: Property<String>

  @TaskAction
  fun execute() {
    val outFile = this.baseDir.get().file("eu/aylett/gradle/generated/ProjectLocations.kt")
    Files.createDirectories(outFile.asFile.parentFile.toPath())
    Files.writeString(
      outFile.asFile.toPath(),
      """
      package eu.aylett.gradle.generated

      const val PROJECT_DIR: String = "${projectDir.get()}"
      """.trimIndent() + '\n',
    )
  }
}

val gen =
  tasks.register<GenerateTestProjectConstants>("generateTestProjectConstants") {
    this.baseDir.set(layout.buildDirectory.dir("generated/test/kotlin"))
    this.projectDir.set(layout.projectDirectory.asFile.canonicalPath)
  }

val isCI = providers.environmentVariable("CI").isPresent

val check = tasks.named("check")
val testing = extensions.getByType<TestingExtension>().apply {
  suites {
    register<JvmTestSuite>("functionalTest") {
      targets.configureEach {
        dependencies {
          implementation(testFixtures(project()))
          implementation(gradleTestKit())
        }
        testTask.configure {
          shouldRunAfter(tasks.named("test"))
        }
      }
    }

    withType<JvmTestSuite>().configureEach {
      useJUnitJupiter(InternalDepsVersions.JUNIT_JUPITER)
      dependencies {
        implementation("org.hamcrest:hamcrest:3.0")
        implementation(gradleApi())
      }
      targets.configureEach {
        sources {
          kotlin {
            srcDir(gen)
          }
        }
        testTask.configure {
          dependsOn(gen)
        }
      }
    }
  }
}

testing.suites.withType<JvmTestSuite>().configureEach {
  check.configure {
    dependsOn( this@configureEach.targets.map { it.testTask })
  }
}
