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

@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  `java-library`
  id("eu.aylett.conventions") version "0.3.0"
  `java-gradle-plugin`
  `kotlin-dsl`
  id("com.diffplug.spotless") version "6.25.0"
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:$embeddedKotlinVersion"))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.10")
  implementation("com.google.guava:guava:32.1.3-jre")
  implementation("eu.aylett:gradle-plugins:0.3.0")
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
  implementation("org.pitest:pitest:1.15.6")
  implementation("com.groupcdg.gradle:common:1.0.7")
  implementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.15.0")
  implementation("com.groupcdg.pitest.github:com.groupcdg.pitest.github.gradle.plugin:1.0.7")
}

tasks.withType<AbstractArchiveTask>().configureEach {
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

@Suppress("INACCESSIBLE_TYPE")
spotless {
  kotlin {
    ktlint()
    target("src/main/kotlin")
  }
  kotlinGradle {
    ktlint()
  }
}

val spotlessApply = tasks.named("spotlessApply")
val spotlessCheck = tasks.named("spotlessCheck")
tasks.named("check").configure { dependsOn(spotlessCheck) }
spotlessApply.configure { mustRunAfter(tasks.named("clean")) }

val isCI = providers.environmentVariable("CI").isPresent
if (!isCI) {
  spotlessCheck.configure { dependsOn(spotlessApply) }
}

val spotlessKotlinApply = tasks.named("spotlessKotlinApply")
val spotlessKotlinCheck = tasks.named("spotlessKotlinCheck")

tasks.withType<KotlinCompilationTask<KotlinCommonCompilerOptions>>().configureEach {
  mustRunAfter(spotlessKotlinApply)
  shouldRunAfter(spotlessKotlinCheck)
}
