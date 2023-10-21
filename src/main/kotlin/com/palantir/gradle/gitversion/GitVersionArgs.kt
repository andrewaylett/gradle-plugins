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

internal class GitVersionArgs {
  var prefix = ""
    set(prefix) {
      Preconditions.checkNotNull(prefix, "prefix must not be null")
      Preconditions.checkState(
        prefix.matches(PREFIX_REGEX.toRegex()),
        "Specified prefix `%s` does not match the allowed format regex `%s`.",
        prefix,
        PREFIX_REGEX,
      )
      field = prefix
    }

  companion object {
    private const val PREFIX_REGEX = "[/@]?([A-Za-z]+[/@-])+"

    // groovy closure invocation allows any number of args
    fun fromGroovyClosure(vararg objects: Any?): GitVersionArgs {
      if (objects.isNotEmpty() && (objects[0] is Map<*, *>)) {
        val instance = GitVersionArgs()
        instance.prefix = (objects[0] as Map<*, *>)["prefix"].toString()
        return instance
      }
      return GitVersionArgs()
    }
  }
}
