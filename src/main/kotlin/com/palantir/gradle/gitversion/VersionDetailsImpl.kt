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

import com.google.common.base.Preconditions
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

internal class VersionDetailsImpl(gitDir: File, args: GitVersionArgs) : VersionDetails {
  private val args: GitVersionArgs
  private val nativeGitInvoker: Git
  private val description: String?

  init {
    val gitDirStr = gitDir.toString()
    val projectDir = gitDirStr.substring(0, gitDirStr.length - DOT_GIT_DIR_PATH.length)
    nativeGitInvoker = Git(File(projectDir))
    this.args = args

    val rawDescription = nativeGitInvoker.describe(args.prefix)
    description = rawDescription?.replaceFirst(("^" + args.prefix + "v?").toRegex(), "")
  }

  override val version: String
    get() =
      if (description == null) {
        "unspecified"
      } else {
        description + if (clean) "" else ".dirty"
      }
  private val clean: Boolean
    get() = nativeGitInvoker.isClean!!

  override val isCleanTag: Boolean
    get() = clean && descriptionIsPlainTag()

  @Deprecated(
    "Provided for Groovy, which likes to strip a level of 'is'",
    ReplaceWith("isCleanTag"),
  )
  override val isIsCleanTag: Boolean
    get() = isCleanTag

  private fun descriptionIsPlainTag(): Boolean {
    return !Pattern.matches(".*g.?[0-9a-fA-F]{3,}", description)
  }

  override val commitDistance: Int
    get() {
      if (descriptionIsPlainTag()) {
        return 0
      }
      val match = Pattern.compile("(.*)-([0-9]+)-g.?[0-9a-fA-F]{3,}").matcher(description)
      Preconditions.checkState(
        match.matches(),
        "Cannot get commit distance for description: '%s'",
        description,
      )
      return match.group(2).toInt()
    }
  override val lastTag: String?
    get() {
      if (descriptionIsPlainTag()) {
        return description
      }
      val match = Pattern.compile("(.*)-([0-9]+)-g.?[0-9a-fA-F]{3,}").matcher(description)
      return if (match.matches()) match.group(1) else null
    }

  @get:Throws(IOException::class)
  override val gitHash: String?
    get() {
      val gitHashFull = gitHashFull ?: return null
      return gitHashFull.substring(0, VERSION_ABBR_LENGTH)
    }

  @get:Throws(IOException::class)
  override val gitHashFull: String?
    get() = nativeGitInvoker.currentHeadFullHash

  @get:Throws(IOException::class)
  override val branchName: String?
    get() = nativeGitInvoker.currentBranch

  override fun toString(): String {
    return try {
      String.format(
        "VersionDetails(%s, %s, %s, %s, %s)",
        version,
        gitHash,
        gitHashFull,
        branchName,
        isCleanTag,
      )
    } catch (e: IOException) {
      ""
    }
  }

  companion object {
    private const val VERSION_ABBR_LENGTH = 10
    private const val DOT_GIT_DIR_PATH = "/.git"
  }
}
