/*
 * Copyright 2025 Andrew Aylett
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

package eu.aylett.gradle.functionaltests.plugins.dependencies

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class AbstractDependencyLockTest {
  @TempDir
  lateinit var testProjectDir: File

  val buildFile: File by lazy { File(testProjectDir, "build.gradle") }
  val lockFile: File by lazy { testProjectDir.resolve("versions.lock") }
  val lockFileLines: List<String> by lazy { lockFile.readLines() }
  val lockEntries: List<String> by lazy {
    lockFileLines.stream().filter { !it.startsWith("#") && !it.isBlank() }.toList()
  }

  val settingsFile: File by lazy { File(testProjectDir, "settings.gradle") }
  val subprojectNames: MutableSet<String> = mutableSetOf()

  lateinit var result: BuildResult

  fun run(vararg args: String) {
    if (subprojectNames.isNotEmpty()) {
      settingsFile.writeText(
        subprojectNames.joinToString("\n") { "include \":$it\"" },
      )
    }

    result =
      GradleRunner
        .create()
        .withProjectDir(testProjectDir)
        .withArguments(*args, "--stacktrace")
        .withPluginClasspath()
        .build()
  }

  fun subproject(name: String): File {
    subprojectNames.add(name)
    val subprojectDir = File(testProjectDir, name)
    subprojectDir.mkdirs()
    return subprojectDir
  }
}
