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

import com.google.common.collect.ImmutableMap
import eu.aylett.gradle.gitversion.JsonUtils.mapToJson
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

internal class JsonUtilsTest {
  @Test
  fun converts_empty_map_to_empty_json_object() {
    val json = mapToJson(emptyMap<String?, Any>())
    assertThat(json, equalTo("{}"))
  }

  @Test
  fun converts_map_of_string_to_long_correctly() {
    val json = mapToJson(ImmutableMap.of("foo", 20, "bar", 40))
    assertThat(json, equalTo("{\"foo\":20,\"bar\":40}"))
  }
}
