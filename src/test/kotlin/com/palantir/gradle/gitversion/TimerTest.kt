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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class TimerTest {
  @Test
  @Throws(JsonProcessingException::class)
  fun generate_correct_json_with_total() {
    val timer = Timer()
    Assertions.assertThat(timer.record("something") { 4 }).isEqualTo(4)
    Assertions.assertThat(timer.record("another") { "foo" }).isEqualTo("foo")
    val objectNode: ObjectNode = OBJECT_MAPPER.readValue(timer.toJson(), ObjectNode::class.java)
    val something: Long = objectNode.get("something").asLong()
    val another: Long = objectNode.get("another").asLong()
    val total: Long = objectNode.get("total").asLong()
    Assertions.assertThat(something).isGreaterThanOrEqualTo(0)
    Assertions.assertThat(another).isGreaterThanOrEqualTo(0)
    Assertions.assertThat(total).isEqualTo(something + another)
    Assertions.assertThat(timer.totalMillis()).isEqualTo(total)
  }

  companion object {
    private val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
  }
}
