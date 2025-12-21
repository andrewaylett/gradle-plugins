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
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask
import java.util.Locale

plugins {
  `java-library`
  `java-gradle-plugin`
  `kotlin-dsl`
  id("org.jmailen.kotlinter") version "5.2.0"
  idea
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

configurations.configureEach {
  resolutionStrategy {
    if (isCanBeConsumed && !isCanBeResolved && !isCanBeDeclared) {
      failOnVersionConflict()
    }
    if (!isCanBeConsumed && !isCanBeResolved && isCanBeDeclared) {
      failOnVersionConflict()
    }
    failOnNonReproducibleResolution()
  }
}

configurations {
  dependencyScope("internalDeps") {
    extendsFrom(configurations.implementation.get())
  }
  resolvable("internalDepsClasspath") {
    shouldResolveConsistentlyWith(configurations.compileClasspath.get())
    extendsFrom(configurations.named("internalDeps").get())
  }
}

val internalDeps by configurations.getting
val internalDepsClasspath by configurations.getting

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.3.0"))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.1.0")
  implementation("com.google.guava:guava:33.5.0-jre")
  implementation("org.jmailen.kotlinter:org.jmailen.kotlinter.gradle.plugin:5.3.0")
  implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:$expectedKotlinDslPluginsVersion")
  implementation("org.pitest:pitest:1.22.0")
  implementation("com.groupcdg.gradle:common:1.0.7")
  implementation("info.solidsoft.gradle.pitest:gradle-pitest-plugin:1.19.0-rc.2")
  implementation("com.arcmutate.github:com.arcmutate.github.gradle.plugin:2.3.2")

  internalDeps("org.junit.jupiter:junit-jupiter:5.14.1")
  internalDeps("org.pitest:pitest-junit5-plugin:1.2.3")
  internalDeps("com.pinterest.ktlint:ktlint-rule-engine:1.7.1")

  constraints {
    implementation("net.minidev:json-smart:2.6.0") {
      because("CVE-2023-1370")
    }
    implementation("commons-io:commons-io:2.21.0") {
      because("CVE-2024-47554")
    }
    implementation("com.fasterxml.jackson.core:jackson-core:2.20.1") {
      because("CVE-2025-52999")
    }
    implementation("org.apache.commons:commons-lang3:3.20.0") {
      because("CVE-2025-48924")
    }
    implementation("com.squareup.okio:okio:3.16.4") {
      because("CVE-2023-3635")
    }
    implementation("com.jayway.jsonpath:json-path") {
      because("CVE-2023-51074")
    }
    implementation("com.fasterxml.woodstox:woodstox-core:7.1.1") {
      because("CVE-2022-40152")
    }
  }
}

val generateInternalDepsVersions by tasks.registering {
  val outputDir = layout.buildDirectory.dir("generated-sources/internal-deps")
  outputs.dir(outputDir)
  dependsOn(internalDepsClasspath)

  doLast {
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
        internalDepsClasspath.incoming.dependencies
          .filter {
            it.name != "unspecified" && it.version != null
          }.forEach {
            appendLine(
              "    const val ${it.name.replace(
                "-",
                "_",
              ).replace(".", "_").uppercase(Locale.getDefault())}: String = \"${it.version}\"",
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

val isCI = providers.environmentVariable("CI").isPresent

tasks.named { it.startsWith("compile") && it.endsWith("Kotlin") }.configureEach {
  val compileSet = name.substringAfter("compile").substringBefore("Kotlin")
  val sourceSet = compileSet.ifEmpty { "Main" }
  mustRunAfter("formatKotlin$sourceSet")
  shouldRunAfter("lintKotlin$sourceSet")
}

val formatKotlinBuildScripts by tasks.registering(FormatTask::class) {
  source(layout.projectDirectory.files("build.gradle.kts", "settings.gradle.kts"))
}

val lintKotlinBuildScripts by tasks.registering(LintTask::class) {
  source(layout.projectDirectory.files("build.gradle.kts", "settings.gradle.kts"))
}

afterEvaluate {
  tasks.named("formatKotlin").configure { dependsOn(formatKotlinBuildScripts) }
  tasks.named("lintKotlin").configure { dependsOn(lintKotlinBuildScripts) }
}

tasks.withType<LintTask>().configureEach {
  group = "verification"
  if (!isCI) {
    dependsOn("format${name.substringAfter("lint")}")
  }
  exclude("gradle/", "eu/aylett/gradle/generated/", "*Plugin.kt")
}

tasks.withType<FormatTask>().configureEach {
  group = "formatting"
  mustRunAfter("clean")
  exclude("gradle/", "eu/aylett/gradle/generated/", "*Plugin.kt")
}

kotlinter {
  ktlintVersion =
    internalDeps.dependencies.find { it.group == "com.pinterest.ktlint" }!!.version!!
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
