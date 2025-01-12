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

import eu.aylett.gradle.generated.InternalDepsVersions
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jmailen.gradle.kotlinter.KotlinterExtension
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
  id("testing")
  id("pitest")
  id("org.gradle.kotlin.kotlin-dsl")
  id("org.jetbrains.dokka")
  id("org.jmailen.kotlinter")
  `java-library`
  idea
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

val implementation by configurations.existing

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:$embeddedKotlinVersion"))
}

tasks.withType<AbstractArchiveTask>().configureEach {
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

val dokka = extensions.getByType<DokkaExtension>()

dokka.apply {
  dokkaSourceSets {
    configureEach {
      includes.from(projectDir.resolve("module.md"))
      jdkVersion.set(21)

      sourceLink {
        localDirectory.set(projectDir.resolve("src"))
        remoteUrl("https://github.com/andrewaylett/gradle-plugins/tree/main/src")
        remoteLineSuffix.set("#L")
//        externalDocumentationLink {
//          url.set(URI("https://docs.gradle.org/${gradle.gradleVersion}/javadoc/"))
//        }
      }
    }
  }
}

val kotlinter = extensions.getByType<KotlinterExtension>()

kotlinter.apply {
  ktlintVersion = InternalDepsVersions.KTLINT_RULE_ENGINE
}

val formatKotlin: TaskProvider<Task> by tasks.existing
val lintKotlin: TaskProvider<Task> by tasks.existing
formatKotlin.configure {
  mustRunAfter(tasks.named("clean"))
}

val isCI = providers.environmentVariable("CI").isPresent
if (!isCI) {
  lintKotlin.configure { dependsOn(formatKotlin) }
}

tasks.configureEach {
  if (name.startsWith("compile") && name.endsWith("Kotlin")) {
    mustRunAfter(formatKotlin)
    shouldRunAfter(lintKotlin)
  }
}

tasks.withType<LintTask> {
  this.source = this.source.minus(fileTree("build/")).asFileTree
}

tasks.withType<FormatTask> {
  this.source = this.source.minus(fileTree("build/")).asFileTree
}

val idea = extensions.getByType<IdeaModel>()

idea.apply {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}
