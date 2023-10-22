/*
 * Copyright 2023 Andrew Aylett
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

import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CanReadLanguageVersion {
  @Test
  fun `configures toolchain`() {
    val project = ProjectBuilder.builder().build()
    project.pluginManager.apply(JavaBasePlugin::class.java)

    val javaToolchain = project.objects.property(JavaLanguageVersion::class.java)
    project.extensions.getByType(JavaPluginExtension::class.java).toolchain {
      // The Java plugin gives us a way to extract this property without
      // setting it to another property. The Kotlin plugin doesn't, but
      // the IDE should infer the type of the block in either case. In
      // practice, IntelliJ is inferring a lambda expression when the
      // type actually required is a function literal with receiver.
      javaToolchain.set(languageVersion)
    }

    assertEquals(17, javaToolchain.get().asInt())
  }
}
