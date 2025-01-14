/*
 * Copyright 2023 Andrew Aylett
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

import java.util.stream.Collectors

internal object JsonUtils {
  fun mapToJson(map: Map<String?, *>): String {
    // Manually writing the json string here rather than using a library to avoid dependencies in this incredibly
    // widely used plugin.
    val middleJson =
      map.entries
        .stream()
        .map { (key, value) ->
          String.format(
            "\"%s\":%s",
            key,
            value.toString(),
          )
        }.collect(Collectors.joining(","))
    return "{$middleJson}"
  }
}
