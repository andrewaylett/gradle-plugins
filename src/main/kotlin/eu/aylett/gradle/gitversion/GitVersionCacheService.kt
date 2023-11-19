/*
 * Copyright 2023 Andrew Aylett
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
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
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

abstract class GitVersionCacheService : BuildService<BuildServiceParameters.None?> {
  private val versionDetailsMap: ConcurrentMap<String, VersionDetails> = ConcurrentHashMap()

  fun getGitVersion(
    project: Path,
    args: Any?,
  ): String {
    val gitDir = getRootGitDir(project)
    val gitVersionArgs: GitVersionArgs = GitVersionArgs.fromGroovyClosure(args)
    val key = gitDir.toString() + "|" + gitVersionArgs.prefix
    val value =
      versionDetailsMap
        .computeIfAbsent(key) { _ -> createVersionDetails(gitDir, gitVersionArgs) }
    return value.version
  }

  fun getVersionDetails(
    project: Path,
    args: Any?,
  ): VersionDetails {
    val gitDir = getRootGitDir(project)
    val gitVersionArgs: GitVersionArgs = GitVersionArgs.fromGroovyClosure(args)
    val key = gitDir.toString() + "|" + gitVersionArgs.prefix
    return versionDetailsMap.computeIfAbsent(
      key,
    ) { _ -> createVersionDetails(gitDir, gitVersionArgs) }
  }

  private fun createVersionDetails(
    gitDir: Path,
    args: GitVersionArgs,
  ): VersionDetails {
    return VersionDetailsImpl(gitDir, args)
  }

  private fun getRootGitDir(currentRoot: Path): Path {
    val gitDir = scanForRootGitDir(currentRoot)
    require(Files.exists(gitDir)) { "Cannot find '.git' directory" }
    return gitDir
  }

  private fun scanForRootGitDir(currentRoot: Path): Path {
    val gitDir = currentRoot.resolve(".git")
    if (Files.exists(gitDir)) {
      return gitDir
    }

    // stop at the root directory, return non-existing Path object;
    return if (currentRoot.parent == null) {
      gitDir
    } else {
      // look in parent directory;
      scanForRootGitDir(currentRoot.parent)
    }
  }

  companion object {
    fun getSharedGitVersionCacheService(project: Project): Provider<GitVersionCacheService> {
      return project.gradle
        .sharedServices
        .registerIfAbsent(
          "GitVersionCacheService",
          GitVersionCacheService::class.java,
        ) {}
    }
  }
}
