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

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.util.function.Function.identity
import java.util.stream.Stream

class GitVersionArgsTest {
  @TestFactory
  fun allowed_prefixes(): Stream<DynamicTest> {
    return DynamicTest.stream(
      Stream.of(
        "@Product@",
        "abc@",
        "abc@test@",
        "Abc-aBc-abC@",
        "foo-bar@",
        "foo-bar/",
        "foo-bar-",
        "foo/bar@",
        "Foo/Bar@",
      ),
      identity(),
    ) { s -> GitVersionArgs().prefix = s }
  }

  @Test
  fun require_dash_or_at_symbol_at_prefix_end() {
    assertThrows<IllegalStateException> { GitVersionArgs().prefix = "v" }
  }
}
