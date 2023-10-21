/*
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
package com.palantir.gradle.gitversion

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.Optional
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GitVersionPluginTests {
  private lateinit var temporaryFolder: Path
  private lateinit var projectDir: Path
  private lateinit var buildFile: Path
  private lateinit var gitIgnoreFile: Path
  private lateinit var dirtyContentFile: Path
  private lateinit var settingsFile: Path

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
    settingsFile.writeText(
      """
      rootProject.name = "gradle-test"
      """.trimIndent(),
    )
    gitIgnoreFile.writeText(".gradle\n")
  }

  @Test
  fun `exception when project root does not have a git repo`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )

    // when:
    val buildResult = with("printVersion").buildAndFail()

    // then:
    buildResult.output.contains("> Cannot find \".git\" directory")
  }

  @Test
  fun `git describe works when git repo is multiple levels up`() {
    // given:
    val rootFolder = temporaryFolder
    projectDir = Files.createDirectories(rootFolder.resolve("level1/level2"))
    buildFile = projectDir.resolve("build.gradle")
    buildFile.createFile()
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    projectDir.resolve("settings.gradle").createFile()
    val git = Git(rootFolder, true)
    git.runGitCommand("init", rootFolder.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // when:
    // will build the project at projectDir
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0\n")
  }

  @Test
  fun `git describe works when using worktree`() {
    // given:
    val rootFolder = temporaryFolder
    projectDir = Files.createDirectories(rootFolder.resolve("worktree"))
    val originalDir = Files.createDirectories(rootFolder.resolve("original"))
    buildFile = originalDir.resolve("build.gradle")
    buildFile.createFile()
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    originalDir.resolve("settings.gradle").createFile()
    val originalGitIgnoreFile = originalDir.resolve(".gitignore")
    originalGitIgnoreFile.createFile()
    originalGitIgnoreFile.writeText(".gradle\n")
    val git = Git(originalDir, true)
    git.runGitCommand("init", originalDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    git.runGitCommand("branch", "newbranch")
    git.runGitCommand("worktree", "add", "../worktree", "newbranch")

    // when:
    // will build the project at projectDir
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0\n")
  }

  @Test
  fun `git version can be applied on sub modules`() {
    // given:
    val subModuleDir =
      Files.createDirectories(
        projectDir
          .resolve("submodule"),
      ).toFile()
    val subModuleBuildFile = File(subModuleDir, "build.gradle")
    subModuleBuildFile.createNewFile()
    subModuleBuildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )

    settingsFile.writeText(
      """
      include "submodule"
      """.trimIndent(),
    )

    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0\n")
  }

  @Test
  fun `unspecified when no tags are present`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )

    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\nunspecified\n")
  }

  @Test
  fun `git describe when annotated tag is present`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0\n")
  }

  @Test
  fun `git describe when lightweight tag is present`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "1.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0\n")
  }

  @Test
  fun `git describe when annotated tag is present with merge commit`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")

    // create repository with a single commit tagged as 1.0.0
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // create a new branch called "hotfix" that has a single commit and is tagged with "1.0.0-hotfix"
    val master = git.currentHeadFullHash!!.substring(0, 7)
    git.runGitCommand("checkout", "-b", "hotfix")
    git.runGitCommand("commit", "-m", "hot fix for issue", "--allow-empty")
    git.runGitCommand("tag", "-a", "1.0.0-hotfix", "-m", "1.0.0-hotfix")
    val commitId = git.currentHeadFullHash!!
    // switch back to main branch and merge hotfix branch into main branch
    git.runGitCommand("checkout", master)
    git.runGitCommand("merge", commitId, "--no-ff", "-m", "merge commit")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.matches(Regex(":printVersion\n1\\.0\\.0-1-g[a-z0-9]{7}\n"))
  }

  @Test
  fun `git describe when annotated tag is present after merge commit`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")

    // create repository with a single commit tagged as 1.0.0
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // create a new branch called "hotfix" that has a single commit and is tagged with "1.0.0-hotfix"

    val master = git.currentHeadFullHash!!.substring(0, 7)
    git.runGitCommand("checkout", "-b", "hotfix")
    git.runGitCommand("commit", "-m", "hot fix for issue", "--allow-empty")
    git.runGitCommand("tag", "-a", "1.0.0-hotfix", "-m", "1.0.0-hotfix")
    val commitId = git.currentHeadFullHash!!

    // switch back to main branch and merge hotfix branch into main branch
    git.runGitCommand("checkout", master)
    git.runGitCommand("merge", commitId, "--no-ff", "-m", "merge commit")

    // tag merge commit on main branch as 2.0.0
    git.runGitCommand("tag", "-a", "2.0.0", "-m", "2.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n2.0.0\n")
  }

  @Test
  fun `git describe and dirty when annotated tag is present and dirty content`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    dirtyContentFile.writeText("dirty-content")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    // buildResult.output.contains(projectDir.getAbsolutePath())
    buildResult.output.contains(":printVersion\n1.0.0.dirty\n")
  }

  @Test
  fun `version details on commit with a tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      task printVersionDetails {
        doLast {
          println versionDetails ().lastTag
          println versionDetails ().commitDistance
          println versionDetails ().gitHash
          println versionDetails ().gitHashFull
          println versionDetails ().branchName
          println versionDetails ().isCleanTag
        }
      }
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    buildResult.output.matches(
      Regex(":printVersionDetails\n1\\.0\\.0\n0\n[a-z0-9]{10}\n[a-z0-9]{40}\nmaster\ntrue\n"),
    )
  }

  @Test
  fun `version details can be accessed using extra properties method`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      task printVersionDetails {
        doLast {
          println project . getExtensions ().getExtraProperties().get("versionDetails")().lastTag
          println project . getExtensions ().getExtraProperties().get("gitVersion")()
        }
      }
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    val sha = git.currentHeadFullHash!!.subSequence(0, 7)

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    buildResult.output.contains(":printVersionDetails\n${sha}\n${sha}\n")
  }

  @Test
  fun `version details when commit distance to tag is gt 0`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      task printVersionDetails {
        doLast {
          println versionDetails ().lastTag
          println versionDetails ().commitDistance
          println versionDetails ().gitHash
          println versionDetails ().branchName
          println versionDetails ().isCleanTag
        }
      }

      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    git.runGitCommand("commit", "-m", "commit 2", "--allow-empty")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    buildResult.output.matches(
      Regex(":printVersionDetails\n1.0.0\n1\n[a-z0-9]{10}\nmaster\nfalse\n"),
    )
  }

  @Test
  fun `isCleanTag should be false when repo dirty on a tag checkout`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      task printVersionDetails {
        doLast {
          println versionDetails ().isCleanTag
        }
      }

      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    dirtyContentFile.writeText("dirty-content")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    buildResult.output.contains(":printVersionDetails\nfalse\n")
  }

  @Test
  fun `version details when detached HEAD mode`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      task printVersionDetails {
        doLast {
          println versionDetails ().lastTag
          println versionDetails ().commitDistance
          println versionDetails ().gitHash
          println versionDetails ().branchName
        }
      }

      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    val commitId = git.currentHeadFullHash
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    git.runGitCommand("commit", "-m", "commit 2", "--allow-empty")
    git.runGitCommand("checkout", commitId!!)

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    buildResult.output.matches(Regex(":printVersionDetails\n1.0.0\n0\n[a-z0-9]{10}\nnull\n"))
  }

  @Test
  fun `version filters out tags not matching prefix and strips prefix`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion (prefix:"my-product@")
      task printVersionDetails {
        doLast {
          println versionDetails (prefix:"my-product@").lastTag
        }
      }
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "my-product@1.0.0", "-m", "my-product@1.0.0")
    git.runGitCommand("commit", "-m", "commit 2", "--allow-empty")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    buildResult.output.contains(":printVersionDetails\n1.0.0\n")
  }

  @Test
  fun `git describe with commit after annotated tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    dirtyContentFile.writeText("dirty-content")
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "add some stuff")
    val commitSha = git.currentHeadFullHash

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0-1-g${commitSha!!.substring(0, 7)}\n")
  }

  @Test
  fun `git describe with commit after lightweight tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "1.0.0")
    dirtyContentFile.writeText("dirty-content")
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "add some stuff")
    val commitSha = git.currentHeadFullHash

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0-1-g${commitSha!!.substring(0, 7)}\n")
  }

  @Test
  fun `test subproject version`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      subprojects {
        apply plugin : "eu.aylett.plugins.version"
        version gitVersion ()
      }
      """.trimIndent(),
    )

    settingsFile.writeText("include \"sub\"")

    gitIgnoreFile.writeText("build\n")
    gitIgnoreFile.writeText("sub\n")

    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    val subDir = Files.createDirectory(temporaryFolder.resolve("sub")).toFile()
    val subGit = Git(subDir, true)
    subGit.runGitCommand("init", subDir.toString())
    val subDirty = File(subDir, "subDirty")
    subDirty.createNewFile()
    subGit.runGitCommand("add", ".")
    subGit.runGitCommand("commit", "-m", "initial commit sub")
    subGit.runGitCommand("tag", "-a", "8.8.8", "-m", "8.8.8")

    // when:
    val buildResult = with("printVersion", ":sub:printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0\n")
    buildResult.output.contains(":sub:printVersion\n8.8.8\n")
  }

  @Test
  fun `test multiple tags on same commit - annotated tag is chosen`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      subprojects {
        apply plugin : "eu.aylett.plugins.version"
        version gitVersion ()
      }
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "1.0.0")
    git.runGitCommand("tag", "-a", "2.0.0", "-m", "2.0.0")
    git.runGitCommand("tag", "3.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n2.0.0\n")
  }

  @Test
  fun `test multiple tags on same commit - most recent annotated tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      subprojects {
        apply plugin : "eu.aylett.plugins.version"
        version gitVersion ()
      }
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    val d1 = Instant.now() - Duration.ofSeconds(1)
    val envvar1 = HashMap<String, String>()
    envvar1["GIT_COMMITTER_DATE"] = d1.toString()
    git.runGitCommand(
      envvar1,
      "-c",
      "user.name=\"name\"",
      "-c",
      "user.email=email@example.com",
      "tag",
      "-a",
      "1.0.0",
      "-m",
      "1.0.0",
    )
    val d2 = Instant.now()
    val envvar2 = HashMap<String, String>()
    envvar2["GIT_COMMITTER_DATE"] = d2.toString()
    git.runGitCommand(
      envvar2,
      "-c",
      "user.name=\"name\"",
      "-c",
      "user.email=email@example.com",
      "tag",
      "-a",
      "2.0.0",
      "-m",
      "2.0.0",
    )
    val d3 = Instant.now() - Duration.ofSeconds(1)
    val envvar3 = HashMap<String, String>()
    envvar3["GIT_COMMITTER_DATE"] = d3.toString()
    git.runGitCommand(
      envvar3,
      "-c",
      "user.name=\"name\"",
      "-c",
      "user.email=email@example.com",
      "tag",
      "-a",
      "3.0.0",
      "-m",
      "3.0.0",
    )

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n2.0.0\n")
  }

  @Test
  fun `test multiple tags on same commit - smaller unannotated tag is chosen`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      subprojects {
        apply plugin : "eu.aylett.plugins.version"
        version gitVersion ()
      }
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "2.0.0")
    git.runGitCommand("tag", "1.0.0")
    git.runGitCommand("tag", "3.0.0")

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0\n")
  }

  @Test
  fun `test tag set on deep commit`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    var latestCommit = git.currentHeadFullHash

    val depth = 100
    for (i in 0..99) {
      git.runGitCommand("add", ".")
      git.runGitCommand("commit", "-m", "commit-$i", "--allow-empty")
      latestCommit = git.currentHeadFullHash
    }

    // when:
    val buildResult = with("printVersion").build()

    // then:
    buildResult.output.contains(":printVersion\n1.0.0-$depth-g${latestCommit!!.substring(0, 7)}\n")
  }

  @Test
  fun `can set build scan custom values when Gradle 6 enterprise plugin 3-2 is applied`() {
    // when:
    settingsFile.writeText(
      """
      plugins {
        id "com.gradle.enterprise" version "3.2"
      }
      """.trimIndent() + '\n' + settingsFile.readText(),
    )

    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // then:
    with("printVersion").build()
  }

  @Test
  fun `can set build scan custom values when Gradle 7 build scan plugin is applied`() {
    // when:
    settingsFile.writeText(
      """
      plugins {
        id "com.gradle.enterprise" version "3.2"
      }
      """.trimIndent() + '\n' + settingsFile.readText(),
    )

    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // then:
    with(Optional.of("7.4.2"), "printVersion").build()
  }

  @Test
  fun `can set build scan custom values when Gradle 6 enterprise plugin 3-1 is applied`() {
    // when:
    settingsFile.writeText(
      """
      plugins {
        id "com.gradle.enterprise" version "3.1"
      }
      """.trimIndent() + '\n' + settingsFile.readText(),
    )

    buildFile.writeText(
      """
      plugins {
        id "eu.aylett.plugins.version"
      }
      version gitVersion ()
      """.trimIndent(),
    )
    gitIgnoreFile.writeText("build")
    val git = Git(projectDir, true)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")

    // then:
    with("printVersion").build()
  }

  private fun with(vararg tasks: String): GradleRunner {
    return with(Optional.empty(), *tasks)
  }

  private fun with(
    gradleVersion: Optional<String>,
    vararg tasks: String,
  ): GradleRunner {
    val arguments = mutableListOf("--stacktrace")
    arguments.addAll(tasks)

    val gradleRunner =
      GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(projectDir.toFile())
        .withArguments(arguments)

    gradleVersion.ifPresent { version -> gradleRunner.withGradleVersion(version) }

    return gradleRunner
  }
}
