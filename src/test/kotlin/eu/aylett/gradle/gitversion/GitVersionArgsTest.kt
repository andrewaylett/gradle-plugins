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

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeAll
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
    ) { s -> GitVersionArgs(s) }
  }

  @Test
  fun require_dash_or_at_symbol_at_prefix_end() {
    assertThrows<IllegalStateException> { GitVersionArgs("v") }
  }

  data class Arg(val arg: Array<Any?>, val prefix: String) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Arg

      return arg.contentEquals(other.arg)
    }

    override fun hashCode(): Int {
      return arg.contentHashCode()
    }
  }

  @TestFactory
  fun `can pass different types of args to make an argument object`(): Stream<DynamicTest> {
    val p = ProjectBuilder.builder().build().providers
    return DynamicTest.stream(
      Stream.of(
        Arg(arrayOf(), ""),
        Arg(arrayOf(mapOf<Any, Any>("prefix" to "")), ""),
        Arg(arrayOf(mapOf<Any, Any>("prefix" to "foo@")), "foo@"),
        Arg(arrayOf(emptyMap<Any, Any>()), ""),
        Arg(arrayOf(""), ""),
        Arg(arrayOf("foo@"), "foo@"),
        Arg(arrayOf(p.provider { "" }), ""),
        Arg(arrayOf(p.provider { "foo@" }), "foo@"),
      ),
      { arg -> "Array '${arg.arg.contentToString()}', Prefix '${arg.prefix}'" },
      { arg ->
        val r = GitVersionArgs.fromGroovyClosure(*arg.arg)
        assertThat(r.prefix, equalTo(arg.prefix))
      },
    )
  }

  @TestFactory
  fun `fails with multiple arguments`(): Stream<DynamicTest> {
    val p = ProjectBuilder.builder().build().providers
    return DynamicTest.stream(
      Stream.of(
        arrayOf(mapOf<Any, Any>("prefix" to "", "bar" to "baz")),
        arrayOf(mapOf<Any, Any>("prefix" to "foo@", "bar" to "baz")),
        arrayOf("", emptyMap<Any, Any>()),
        arrayOf("", ""),
        arrayOf("foo@", ""),
        arrayOf("", p.provider { "" }),
        arrayOf(p.provider { "foo@" }, ""),
      ),
      { arg -> "Array '${arg.contentToString()}'" },
      { arg ->
        assertThrows<IllegalArgumentException> { GitVersionArgs.fromGroovyClosure(*arg) }
      },
    )
  }

  companion object {
    @JvmStatic
    @BeforeAll
    fun `ensure global services is initialised`() {
      try {
        ProjectBuilder.builder().build()
      } catch (e: GradleException) {
        assertThat(e.message, equalTo("Could not inject synthetic classes."))
      }
    }
  }
}
