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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GitVersionArgsTest {
  @Test
  fun allowed_prefixes() {
    GitVersionArgs().prefix = "@Product@"
    GitVersionArgs().prefix = "abc@"
    GitVersionArgs().prefix = "abc@test@"
    GitVersionArgs().prefix = "Abc-aBc-abC@"
    GitVersionArgs().prefix = "foo-bar@"
    GitVersionArgs().prefix = "foo-bar/"
    GitVersionArgs().prefix = "foo-bar-"
    GitVersionArgs().prefix = "foo/bar@"
    GitVersionArgs().prefix = "Foo/Bar@"
  }

  @Test
  fun require_dash_or_at_symbol_at_prefix_end() {
    assertThrows<IllegalStateException> { GitVersionArgs().prefix = "v" }
  }
}
