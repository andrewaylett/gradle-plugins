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
package eu.aylett.gradle.gitversion

import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern
import kotlin.io.path.writeText

private val HASH_AND_DIRTY_REGEX: Pattern = Pattern.compile("[0-9a-f]{7}\\.dirty")
private val HASH_REGEX: Pattern = Pattern.compile("[0-9a-f]{7}")

class VersionDetailsTest {
  @TempDir
  lateinit var temporaryFolder: Path
  private lateinit var git: NativeGit
  private val formattedTime = "'2005-04-07T22:13:13'"
  private val project: Project by lazy {
    ProjectBuilder.builder().withProjectDir(temporaryFolder.toFile()).build()
  }

  @BeforeEach
  fun before() {
    git = NativeGit(temporaryFolder)
    git.runGitCommand("init", temporaryFolder.toString())
    temporaryFolder.resolve(".gitignore").writeText(".gradle\nuserHome\n")
  }

  @Test
  fun `symlinks should result in clean git tree`() {
    val fileToLinkTo = write(temporaryFolder.resolve("fileToLinkTo"))
    Files.createSymbolicLink(
      temporaryFolder.resolve("fileLink"),
      fileToLinkTo,
    )
    val folderToLinkTo = temporaryFolder.resolve("folderToLinkTo")
    assertThat(Files.createDirectories(folderToLinkTo), notNullValue())
    write(folderToLinkTo.resolve("dummyFile"))
    Files.createSymbolicLink(
      temporaryFolder.resolve("folderLink"),
      folderToLinkTo,
    )
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "'initial commit'")
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "unused")
    assertThat("In $temporaryFolder", versionDetails().version, equalTo("1.0.0"))
  }

  @Test
  fun `unspecified when no commit exists`() {
    assertThat("In $temporaryFolder", versionDetails().version, equalTo("unspecified"))
  }

  @Test
  fun `multiple tags picks the annotated tag`() {
    temporaryFolder.resolve("file").writeText("Content")
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "1.0.0")
    git.runGitCommand("tag", "-a", "2.0.0", "-m", "2.0.0")
    git.runGitCommand("tag", "3.0.0")
    assertThat("In $temporaryFolder", versionDetails().version, equalTo("2.0.0"))
  }

  @Test
  fun `test multiple tags on same commit - most recent annotated tag`() {
    // given:
    temporaryFolder.resolve("file").writeText("Content")
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    val now = Instant.now()
    val d1 = now - Duration.ofSeconds(1)
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
    val d2 = now
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
    val d3 = now - Duration.ofSeconds(1)
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

    // then:
    assertThat("In $temporaryFolder", versionDetails().version, equalTo("2.0.0"))
  }

  @Test
  fun `short sha when no annotated tags are present`() {
    git.runGitCommand("add", ".")
    val envvar: MutableMap<String, String> = HashMap()
    envvar["GIT_COMMITTER_DATE"] = formattedTime
    envvar["TZ"] = "UTC"
    git.runGitCommand(
      envvar,
      "-c",
      "user.name='name'",
      "-c",
      "user.email=email@address",
      "commit",
      "--author='name <email@address>'",
      "-m",
      "'initial commit'",
      "--date=$formattedTime",
      "--allow-empty",
    )
    assertThat("In $temporaryFolder", versionDetails().version, matchesRegex(HASH_REGEX))
  }

  @Test
  fun short_sha_when_no_annotated_tags_are_present_and_dirty_content() {
    git.runGitCommand("add", ".")
    val envvar: MutableMap<String, String> = HashMap()
    envvar["GIT_COMMITTER_DATE"] = formattedTime
    git.runGitCommand(
      envvar,
      "-c",
      "user.name='name'",
      "-c",
      "user.email=email@address",
      "commit",
      "--author='name <email@address>'",
      "-m",
      "'initial commit'",
      "--date=$formattedTime",
      "--allow-empty",
    )
    write(temporaryFolder.resolve("foo"))
    assertThat(versionDetails().version, matchesRegex(HASH_AND_DIRTY_REGEX))
  }

  @Test
  fun git_version_result_is_being_cached() {
    write(temporaryFolder.resolve("foo"))
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "v1.0.0", "-m", "cached")
    val versionDetails = versionDetails()
    assertThat(versionDetails.version, equalTo("1.0.0"))
    git.runGitCommand("tag", "-a", "2.0.0", "-m", "unused")
    assertThat(versionDetails.version, equalTo("1.0.0"))
  }

  @Throws(IOException::class)
  private fun write(file: Path): Path {
    Files.writeString(file, "content")
    return file
  }

  private fun versionDetails(): VersionDetails {
    val gitDir = temporaryFolder
    return VersionDetailsImpl(gitDir, GitVersionArgs(), project.providers)
  }
}
