/*
 * Copyright 2023 Andrew Aylett
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package eu.aylett.gradle.functionaltests.gitversion

import eu.aylett.gradle.gitversion.NativeGit
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInRelativeOrder
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import kotlin.io.path.writeText

class GitVersionPluginWorktreeRepositoryTests : GitVersionPluginTests(false, "original") {
  @Test
  fun `git describe fails when using worktree`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    val git = NativeGit(projectDir)
    git.runGitCommand("init", projectDir.toFile().absolutePath)
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    git.runGitCommand("branch", "newbranch")
    val worktreePath = "../worktree"
    git.runGitCommand("worktree", "add", worktreePath, "newbranch")

    // when:
    // will build the project at projectDir
    val buildResult =
      with("printVersion")
        .withProjectDir(projectDir.resolve(worktreePath).toFile())
        .buildAndFail()

    // then:
    assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder(containsString("Cannot find git repository")),
    )
  }
}
