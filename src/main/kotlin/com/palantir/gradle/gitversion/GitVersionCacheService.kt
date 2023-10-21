/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
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

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

abstract class GitVersionCacheService : BuildService<BuildServiceParameters.None?> {
  private val timer = Timer()
  private val versionDetailsMap: ConcurrentMap<String, VersionDetails?> = ConcurrentHashMap()

  fun getGitVersion(
    project: File,
    args: Any?,
  ): String? {
    val gitDir = getRootGitDir(project)
    val gitVersionArgs: GitVersionArgs = GitVersionArgs.fromGroovyClosure(args)
    val key = gitDir.toPath().toString() + "|" + gitVersionArgs.prefix
    val value =
      versionDetailsMap
        .computeIfAbsent(key) { _ -> createVersionDetails(gitDir, gitVersionArgs) }
    return value?.version
  }

  fun getVersionDetails(
    project: File,
    args: Any?,
  ): VersionDetails? {
    val gitDir = getRootGitDir(project)
    val gitVersionArgs: GitVersionArgs = GitVersionArgs.fromGroovyClosure(args)
    val key = gitDir.toPath().toString() + "|" + gitVersionArgs.prefix
    return versionDetailsMap.computeIfAbsent(
      key,
    ) { _ -> createVersionDetails(gitDir, gitVersionArgs) }
  }

  private fun createVersionDetails(
    gitDir: File,
    args: GitVersionArgs,
  ): VersionDetails {
    return VersionDetailsImpl(gitDir, args)
  }

  fun timer(): Timer {
    return timer
  }

  companion object {
    private val log = LoggerFactory.getLogger(GitVersionCacheService::class.java)

    private fun getRootGitDir(currentRoot: File): File {
      val gitDir = scanForRootGitDir(currentRoot)
      require(gitDir.exists()) { "Cannot find '.git' directory" }
      return gitDir
    }

    private fun scanForRootGitDir(currentRoot: File): File {
      val gitDir = File(currentRoot, ".git")
      if (gitDir.exists()) {
        return gitDir
      }

      // stop at the root directory, return non-existing File object;
      return if (currentRoot.getParentFile() == null) {
        gitDir
      } else {
        // look in parent directory;
        scanForRootGitDir(currentRoot.getParentFile())
      }
    }

    fun getSharedGitVersionCacheService(project: Project): Provider<GitVersionCacheService> {
      return project.gradle
        .sharedServices
        .registerIfAbsent(
          "GitVersionCacheService",
          GitVersionCacheService::class.java,
          {},
        )
    }
  }
}
