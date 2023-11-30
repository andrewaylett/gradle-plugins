/*
 * Copyright 2023-2024 Andrew Aylett
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

import org.gradle.api.plugins.PluginAware
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class HasPluginMatcher(private val pluginName: String) : TypeSafeMatcher<PluginAware>() {
  override fun describeTo(description: Description) {
    description.apply {
      appendText("gradle plugin manager containing plugin ")
      appendText(pluginName)
    }
  }

  override fun matchesSafely(item: PluginAware): Boolean {
    return item.pluginManager.hasPlugin(pluginName)
  }

  override fun describeMismatchSafely(
    item: PluginAware,
    mismatchDescription: Description,
  ) {
    mismatchDescription.apply {
      appendText("gradle plugin manager did not contain $pluginName, but ")
      appendValueList("[", ", ", "]", item.plugins)
    }
  }
}

fun hasPlugin(pluginName: String): HasPluginMatcher {
  return HasPluginMatcher(pluginName)
}
