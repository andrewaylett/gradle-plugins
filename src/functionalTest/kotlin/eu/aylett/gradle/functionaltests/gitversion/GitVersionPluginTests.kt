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

import eu.aylett.gradle.gitversion.NativeGit
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.util.Optional
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

abstract class GitVersionPluginTests(
  kotlin: Boolean,
  projectDirRelative: String = "",
) {
  constructor() : this(false)

  protected val temporaryFolder: Path by lazy { createTempDirectory("GitVersionPluginTest") }
  protected val projectDir: Path by lazy {
    createDirectories(temporaryFolder.resolve(projectDirRelative))
  }
  private val kts: String =
    if (kotlin) {
      ".kts"
    } else {
      ""
    }
  protected val buildFile: Path by lazy { projectDir.resolve("build.gradle$kts").createFile() }
  protected val gitIgnoreFile: Path by lazy { projectDir.resolve(".gitignore").createFile() }
  protected val dirtyContentFile: Path by lazy { projectDir.resolve("dirty").createFile() }
  protected val settingsFile: Path by lazy {
    projectDir
      .resolve(
        "settings.gradle$kts",
      ).createFile()
  }
  protected val propertiesFile: Path by lazy {
    projectDir
      .resolve(
        "gradle.properties",
      ).createFile()
  }

  @BeforeEach
  fun setup() {
    settingsFile.writeText("rootProject.name = \"gradle-test\"\n")
    gitIgnoreFile.writeText(".gradle\n")
  }

  protected fun with(vararg tasks: String): GradleRunner = with(Optional.empty(), *tasks)

  protected fun with(
    gradleVersion: Optional<String>,
    vararg tasks: String,
  ): GradleRunner {
    val arguments = mutableListOf("--stacktrace", "--info")
    arguments.addAll(tasks)

    val gradleRunner =
      GradleRunner
        .create()
        .forwardOutput()
        .withDebug(true)
        .withPluginClasspath()
        .withProjectDir(projectDir.toFile())
        .withArguments(arguments)

    gradleVersion.ifPresent { version -> gradleRunner.withGradleVersion(version) }

    return gradleRunner
  }

  protected fun git(
    projectDir: Path,
    action: NativeGit.() -> Unit,
  ): NativeGit {
    val git = NativeGit(projectDir)
    action.invoke(git)
    return git
  }
}
