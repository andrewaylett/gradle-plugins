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

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files

class VersionDetailsTest {
  @TempDir
  var temporaryFolder: File? = null
  private var git: Git? = null
  val formattedTime = "'2005-04-07T22:13:13'"

  @BeforeEach
  fun before() {
    git = Git(temporaryFolder!!, true)
    git!!.runGitCommand("init", temporaryFolder.toString())
  }

  @Test
  fun `symlinks should result in clean git tree`() {
    val fileToLinkTo = write(File(temporaryFolder, "fileToLinkTo"))
    Files.createSymbolicLink(
      temporaryFolder!!.toPath().resolve("fileLink"),
      fileToLinkTo.toPath(),
    )
    val folderToLinkTo = File(temporaryFolder, "folderToLinkTo")
    Assertions.assertThat(folderToLinkTo.mkdir()).isTrue()
    write(File(folderToLinkTo, "dummyFile"))
    Files.createSymbolicLink(
      temporaryFolder!!.toPath().resolve("folderLink"),
      folderToLinkTo.toPath(),
    )
    git!!.runGitCommand("add", ".")
    git!!.runGitCommand("commit", "-m", "'initial commit'")
    git!!.runGitCommand("tag", "-a", "1.0.0", "-m", "unused")
    assertThat(versionDetails().version).isEqualTo("1.0.0")
  }

  @Test
  fun `short sha when no annotated tags are present`() {
    git!!.runGitCommand("add", ".")
    val envvar: MutableMap<String, String> = HashMap()
    envvar["GIT_COMMITTER_DATE"] = formattedTime
    envvar["TZ"] = "UTC"
    git!!.runGitCommand(
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
    assertThat(versionDetails().version).matches("[0-9a-f]{7}")
  }

  @Test
  fun short_sha_when_no_annotated_tags_are_present_and_dirty_content() {
    git!!.runGitCommand("add", ".")
    val envvar: MutableMap<String, String> = HashMap()
    envvar["GIT_COMMITTER_DATE"] = formattedTime
    git!!.runGitCommand(
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
    write(File(temporaryFolder, "foo"))
    assertThat(versionDetails().version).matches("[0-9a-f]{7}\\.dirty")
  }

  @Test
  fun git_version_result_is_being_cached() {
    write(File(temporaryFolder, "foo"))
    git!!.runGitCommand("add", ".")
    git!!.runGitCommand("commit", "-m", "initial commit")
    git!!.runGitCommand("tag", "-a", "v1.0.0", "-m", "cached")
    val versionDetails = versionDetails()
    assertThat(versionDetails.version).isEqualTo("1.0.0")
    git!!.runGitCommand("tag", "-a", "2.0.0", "-m", "unused")
    assertThat(versionDetails.version).isEqualTo("1.0.0")
  }

  @Throws(IOException::class)
  private fun write(file: File): File {
    Files.writeString(file.toPath(), "content")
    return file
  }

  private fun versionDetails(): VersionDetails {
    val gitDir = temporaryFolder.toString() + "/.git"
    return VersionDetailsImpl(File(gitDir), GitVersionArgs())
  }
}
