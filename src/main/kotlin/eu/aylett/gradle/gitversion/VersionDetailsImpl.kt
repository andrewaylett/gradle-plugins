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

import org.gradle.kotlin.dsl.provideDelegate
import java.nio.file.Path

private val plainTagRegex: Regex by lazy { Regex(".*g.?[0-9a-fA-F]{3,}") }
private val descriptionDistanceRegex: Regex by lazy {
  Regex("(.*)-([0-9]+)-g.?[0-9a-fA-F]{3,}")
}

internal class VersionDetailsImpl(
  private val gitDir: Path,
  private val args: GitVersionArgs,
  private val isolateGit: Boolean,
) : VersionDetails {
  private val nativeGitInvoker: Git by lazy { Git(gitDir.parent, isolateGit) }

  private val description: String by lazy {
    nativeGitInvoker
      .describe(args.prefix)
      .replaceFirst(prefixPattern, "")
  }

  private val prefixPattern: Regex by lazy {
    Regex("^" + Regex.escape(args.prefix) + "v?")
  }

  override val version: String by lazy {
    try {
      if (description.isBlank()) {
        "unspecified"
      } else if (clean) {
        description
      } else {
        "$description.dirty"
      }
    } catch (e: GitException) {
      if (e.statusCode != null) {
        "unspecified"
      } else {
        throw GitException(null, e, null)
      }
    }
  }

  private val clean: Boolean by lazy {
    try {
      nativeGitInvoker.isClean
    } catch (e: GitException) {
      if (e.statusCode != null) {
        false
      } else {
        throw GitException(null, e, null)
      }
    }
  }

  override val isCleanTag: Boolean by lazy { clean && descriptionIsPlainTag }

  @Deprecated(
    "Provided for Groovy, which likes to strip a level of 'is'",
    ReplaceWith("isCleanTag"),
  )
  override val isIsCleanTag: Boolean
    get() = isCleanTag

  private val descriptionIsPlainTag: Boolean by lazy {
    try {
      !plainTagRegex.matches(description)
    } catch (e: GitException) {
      if (e.statusCode != null) {
        false
      } else {
        throw GitException(null, e, null)
      }
    }
  }

  override val commitDistance: Int by lazy {
    if (descriptionIsPlainTag) {
      0
    } else {
      val match =
        checkNotNull(descriptionDistanceRegex.matchEntire(description)) {
          "Cannot get commit distance for description: '$description'"
        }
      match.groups[2]!!.value.toInt()
    }
  }

  override val lastTag: String by lazy {
    if (descriptionIsPlainTag) {
      description
    } else {
      val match = descriptionDistanceRegex.matchEntire(description)
      if (match != null) {
        match.groups[1]!!.value
      } else {
        ""
      }
    }
  }

  override val gitHash: String by lazy {
    gitHashFull.substring(0, VERSION_ABBR_LENGTH)
  }

  override val gitHashFull: String by lazy { nativeGitInvoker.currentHeadFullHash }

  override val branchName: String by lazy { nativeGitInvoker.currentBranch }

  override fun toString(): String =
    "VersionDetails($version, $gitHash, $gitHashFull, $branchName, $isCleanTag)"
}

private const val VERSION_ABBR_LENGTH = 10
