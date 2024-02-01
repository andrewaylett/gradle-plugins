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

import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.BaseKotlinCompile

plugins {
  java
  id("org.gradle.kotlin.kotlin-dsl")
  id("org.jetbrains.kotlin.jvm")
  id("com.diffplug.spotless")
}

val check = tasks.named("check")
extensions.getByType<SpotlessExtension>().apply {
  kotlin {
    ktlint()
    target(sourceSets.map { it.kotlin.sourceDirectories })
    targetExclude(layout.buildDirectory.dir("generated/test/kotlin"))
  }
  kotlinGradle {
    ktlint()
  }
}

val spotlessApply = tasks.named("spotlessApply")
val spotlessCheck = tasks.named("spotlessCheck")
check.configure { dependsOn(spotlessCheck) }
tasks.withType<AbstractCompile>().configureEach {
  shouldRunAfter(spotlessCheck)
  mustRunAfter(spotlessApply)
}

val isCI = providers.environmentVariable("CI").isPresent
if (!isCI) {
  spotlessCheck.configure { dependsOn(spotlessApply) }
}

tasks.withType<BaseKotlinCompile>().configureEach {
  mustRunAfter(tasks.named("spotlessKotlinApply"))
  shouldRunAfter(tasks.named("spotlessKotlinCheck"))
}
