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

@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion
import java.util.Locale
import org.jmailen.gradle.kotlinter.tasks.LintTask
import org.jmailen.gradle.kotlinter.tasks.FormatTask

plugins {
  `java-library`
  `java-gradle-plugin`
  `kotlin-dsl`
  id("org.jmailen.kotlinter") version "5.0.2"
  idea
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

val internalDeps: Configuration by configurations.creating {
  isCanBeDeclared = true
  isCanBeConsumed = false
  isCanBeResolved = true
  shouldResolveConsistentlyWith(configurations.compileClasspath.get())
  extendsFrom(configurations.implementation.get())
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:$embeddedKotlinVersion"))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
  implementation("com.google.guava:guava:33.4.8-jre")
  implementation("org.jmailen.kotlinter:org.jmailen.kotlinter.gradle.plugin:5.0.2")
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
  implementation("org.pitest:pitest:1.17.4")
  implementation("com.groupcdg.gradle:common:1.0.7")
  implementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.15.0")
  implementation("com.groupcdg.pitest.github:com.groupcdg.pitest.github.gradle.plugin:1.0.7")

  internalDeps("org.junit.jupiter:junit-jupiter:5.11.4")
  internalDeps("org.pitest:pitest-junit5-plugin:1.2.3")
  internalDeps("com.pinterest.ktlint:ktlint-rule-engine:1.6.0")
}

val generateInternalDepsVersions by tasks.registering {
  val outputDir = layout.buildDirectory.dir("generated-sources/internal-deps")
  outputs.dir(outputDir)
  dependsOn(internalDeps)

  doLast {
    internalDeps.resolve()
    val versionsFile =
      outputDir
        .get()
        .file(
          "eu/aylett/gradle/generated/InternalDepsVersions.kt",
        ).asFile
    versionsFile.parentFile.mkdirs()
    versionsFile.writeText(
      buildString {
        appendLine("package eu.aylett.gradle.generated")
        appendLine()
        appendLine("@Suppress(\"unused\")")
        appendLine("object InternalDepsVersions {")
        internalDeps.allDependencies
          .filter {
            it.name != "unspecified" &&
              !it.name.contains(
                ".",
              )
          }.forEach {
            appendLine(
              "    const val ${it.name.replace(
                "-",
                "_",
              ).uppercase(Locale.getDefault())}: String = \"${it.version}\"",
            )
          }
        appendLine("}")
      },
    )
  }
}

tasks.withType<AbstractArchiveTask>().configureEach {
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

val formatKotlin: TaskProvider<Task> by tasks.existing
val lintKotlin: TaskProvider<Task> by tasks.existing
formatKotlin.configure { mustRunAfter(tasks.named("clean"))
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

kotlinter {
  ktlintVersion = internalDeps.dependencies.find { it.group == "com.pinterest.ktlint" }!!.version!!
}

configurations.matching { it.isCanBeConsumed && !it.isCanBeResolved }.configureEach {
  resolutionStrategy {
    failOnVersionConflict()
  }
}

configurations.configureEach {
  resolutionStrategy {
    failOnNonReproducibleResolution()
  }
}

java {
  consistentResolution {
    useCompileClasspathVersions()
  }
}

sourceSets {
  main {
    kotlin {
      srcDir(generateInternalDepsVersions)
    }
  }
}

idea {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}
