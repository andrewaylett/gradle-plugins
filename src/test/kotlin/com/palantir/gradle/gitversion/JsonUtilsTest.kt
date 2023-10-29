/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

import com.google.common.collect.ImmutableMap
import com.palantir.gradle.gitversion.JsonUtils.mapToJson
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class JsonUtilsTest {
  @Test
  fun converts_empty_map_to_empty_json_object() {
    val json = mapToJson(emptyMap<String?, Any>())
    Assertions.assertThat(json).isEqualTo("{}")
  }

  @Test
  fun converts_map_of_string_to_long_correctly() {
    val json = mapToJson(ImmutableMap.of("foo", 20, "bar", 40))
    Assertions.assertThat(json).isEqualTo("{\"foo\":20,\"bar\":40}")
  }
}
