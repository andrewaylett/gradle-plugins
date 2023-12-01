/*
 * Copyright 2023 Andrew Aylett
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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Constants.R_TAGS
import org.eclipse.jgit.lib.Repository.shortenRefName
import org.eclipse.jgit.revwalk.RevWalk
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.isDirectory

class Git(gitDir: Path) : AutoCloseable {
  init {
    if (!gitDir.isDirectory()) {
      throw UnsupportedOperationException(
        "Cannot find git repository.  Detached work trees are unsupported.",
      )
    }
  }

  private val repository: FileRepository by lazy { FileRepository(gitDir.toFile()) }
  private val git: Git by lazy { Git(repository) }
  val currentBranch: String by lazy {
    val head = repository.exactRef("HEAD")
    if (head.isSymbolic) {
      shortenRefName(head.target.name)
    } else {
      ""
    }
  }
  val currentHeadFullHash: String by lazy { repository.findRef("HEAD").objectId.name }
  val isClean: Boolean by lazy { git.status().call().isClean }

  fun describe(prefix: String): String {
    /*
      "--tags",
      "--always",
      "--first-parent",
      "--abbrev=7",
      "--match=$prefix*",
      "HEAD",
     */
    val head = repository.findRef("HEAD")

    val objectId = head?.objectId ?: return ""

    val shortHash = objectId.name.substring(0..6)

    val tagList = repository.refDatabase.getRefsByPrefix(R_TAGS + prefix)
    val tags =
      tagList.stream().collect(
        Collectors.groupingBy {
          repository.refDatabase.peel(it).peeledObjectId ?: it.objectId
        },
      )

    val walker = RevWalk(repository)
    walker.isFirstParent = true
    var candidate = walker.parseCommit(head.objectId)
    var candidateDistance = 0
    while (candidate.parentCount > 0) {
      if (candidate.id in tags) {
        break
      }
      candidateDistance += 1
      candidate = repository.parseCommit(candidate.getParent(0))
    }

    val candidateRefs =
      tags[candidate.id]
        ?: return if (candidateDistance == 0) shortHash else "$candidateDistance-g$shortHash"

    val annotatedTags =
      candidateRefs.filter {
        it.objectId != candidate.id
      }

    val tag =
      annotatedTags.maxByOrNull {
        walker.parseTag(it.objectId).taggerIdent.whenAsInstant
      } ?: candidateRefs[0]

    val shortTag = tag.name.substringAfter("refs/tags/")
    return if (candidateDistance == 0) {
      shortTag
    } else {
      "$shortTag-$candidateDistance-g$shortHash"
    }
  }

  override fun close() {
    git.close()
    repository.close()
  }
}
