/*
 * Copyright 2023 Andrew Aylett
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.aylett.gradle.functionaltests.gitversion

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
abstract class GitVersionPluginTests {
  protected lateinit var temporaryFolder: Path
  protected lateinit var projectDir: Path
  protected lateinit var buildFile: Path
  protected lateinit var gitIgnoreFile: Path
  protected lateinit var dirtyContentFile: Path
  protected lateinit var settingsFile: Path

  @BeforeEach
  fun setup() {
    temporaryFolder = createTempDirectory("GitVersionPluginTest")
    projectDir = temporaryFolder
    buildFile = temporaryFolder.resolve("build.gradle")
    buildFile.createFile()
    settingsFile = temporaryFolder.resolve("settings.gradle")
    settingsFile.createFile()
    gitIgnoreFile = temporaryFolder.resolve(".gitignore")
    gitIgnoreFile.createFile()
    dirtyContentFile = temporaryFolder.resolve("dirty")
    dirtyContentFile.createFile()
    settingsFile.writeText("rootProject.name = \"gradle-test\"\n")
    gitIgnoreFile.writeText(".gradle\n")
  }

  protected fun with(vararg tasks: String): GradleRunner {
    return with(Optional.empty(), *tasks)
  }

  protected fun with(
    gradleVersion: Optional<String>,
    vararg tasks: String,
  ): GradleRunner {
    val arguments = mutableListOf("--stacktrace")
    arguments.addAll(tasks)

    val gradleRunner =
      GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withProjectDir(projectDir.toFile())
        .withArguments(arguments)

    gradleVersion.ifPresent { version -> gradleRunner.withGradleVersion(version) }

    return gradleRunner
  }
}
