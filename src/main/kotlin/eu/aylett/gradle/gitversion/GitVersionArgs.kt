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

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Preconditions.checkState
import org.gradle.api.provider.Provider

internal class GitVersionArgs(
  val prefix: String = "",
) {
  init {
    checkNotNull(prefix, "prefix must not be null")
    checkState(
      prefix.matches(PREFIX_REGEX.toRegex()),
      "Specified prefix `%s` does not match the allowed format regex `%s`.",
      prefix,
      PREFIX_REGEX,
    )
  }

  companion object {
    private const val PREFIX_REGEX = "|([/@]?([A-Za-z]+[/@-])+)"

    // groovy closure invocation allows any number of args
    fun fromGroovyClosure(vararg objects: Any?): GitVersionArgs =
      if (objects.size == 1) {
        val arg = objects[0]
        if (arg is Map<*, *>) {
          if (arg.isEmpty()) {
            GitVersionArgs()
          } else if (arg.size == 1 && arg.containsKey("prefix")) {
            GitVersionArgs(arg["prefix"].toString())
          } else {
            throw IllegalArgumentException("Closure only accepts a prefix argument")
          }
        } else if (arg is Provider<*>) {
          GitVersionArgs(arg.get().toString())
        } else {
          GitVersionArgs(arg.toString())
        }
      } else if (objects.isEmpty()) {
        GitVersionArgs()
      } else {
        throw IllegalArgumentException("Closure expects at most one argument")
      }

    fun fromProvider(prop: Provider<String>): GitVersionArgs {
      if (prop.isPresent) {
        return GitVersionArgs(prop.get())
      }
      return GitVersionArgs()
    }
  }
}
