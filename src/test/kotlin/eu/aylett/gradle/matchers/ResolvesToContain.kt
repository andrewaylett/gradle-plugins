/*
 * Copyright 2023-2025 Andrew Aylett
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

package eu.aylett.gradle.matchers

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.hamcrest.SelfDescribing
import org.hamcrest.TypeSafeMatcher
import java.util.List.copyOf

class ResolvesToContain(
  dependencies: List<ExternalModuleDependency>,
) : TypeSafeMatcher<Set<ResolvedArtifact>>() {
  private val dependencies = copyOf(dependencies)

  override fun describeTo(description: org.hamcrest.Description) {
    val selfDescribing =
      dependencies.map {
        SelfDescribing { d -> d.appendText("(${it.group}:${it.name}:${it.version})") }
      }
    description.appendText("A configuration that includes dependencies on ")
    description.appendList("<[", ", ", "]>", selfDescribing)
  }

  override fun describeMismatchSafely(
    item: Set<ResolvedArtifact>,
    mismatchDescription: org.hamcrest.Description,
  ) {
    mismatchDescription.appendText("was missing ")
    val mismatches = mutableListOf<SelfDescribing>()
    for (required in dependencies) {
      if (!item.any { required.matchesStrictly(it.moduleVersion.id) }) {
        mismatches.add(
          SelfDescribing { d ->
            d.appendText("(${required.group}:${required.name}:${required.version})")
          },
        )
      }
    }
    mismatchDescription.appendList("<[", ", ", "]> from ", mismatches)
    mismatchDescription.appendValue(item)
  }

  override fun matchesSafely(item: Set<ResolvedArtifact>): Boolean {
    for (required in dependencies) {
      if (!item.any { required.matchesStrictly(it.moduleVersion.id) }) {
        return false
      }
    }
    return true
  }
}
