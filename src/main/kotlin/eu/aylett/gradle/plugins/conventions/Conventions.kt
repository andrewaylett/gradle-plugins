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

package eu.aylett.gradle.plugins.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project

class Conventions : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.run {
      apply(BomAlignmentConvention::class.java)
      apply(IDESupportConvention::class.java)
      apply(JvmConvention::class.java)
    }
  }
}
