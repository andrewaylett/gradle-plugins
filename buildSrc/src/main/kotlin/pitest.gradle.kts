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

import eu.aylett.gradle.generated.InternalDepsVersions


plugins {
  `jvm-test-suite`
  id("testing")
  id("info.solidsoft.pitest")
  id("com.arcmutate.github")
}

dependencies {
  pitest("org.slf4j:slf4j-api:2.0.17")
  pitest("ch.qos.logback:logback-classic:1.5.21")
  pitest("com.arcmutate:base:1.7.0")
  pitest("com.arcmutate:pitest-accelerator-junit5:1.2.2")
  pitest("com.arcmutate:pitest-git-plugin:2.2.5")
  pitest("com.arcmutate:pitest-kotlin-plugin:1.5.0")
}

val isCI = providers.environmentVariable("CI").isPresent
pitest {
  targetClasses.add("eu.aylett.*")
  // Not much point in running functional tests, they don't pick up mutations
  // testSourceSets.set(testing.suites.withType<JvmTestSuite>().transform { it.sources })
  // Don't mutate the class that calls git
  excludedClasses.add("eu.aylett.gradle.gitversion.Git")

  junit5PluginVersion = InternalDepsVersions.PITEST_JUNIT5_PLUGIN
  verbosity = "VERBOSE_NO_SPINNER"
  pitestVersion = InternalDepsVersions.PITEST
  failWhenNoMutations = false
  mutators = listOf("STRONGER", "EXTENDED")
  timeoutFactor = BigDecimal.TEN

  exportLineCoverage = true
  features.add("+auto_threads")
  if (isCI) {
    // Running in GitHub Actions
    features.addAll("+git(from[HEAD~1])", "+gitci(level[warning])")
    outputFormats = listOf("html", "xml", "gitci")
    failWhenNoMutations = false
  } else {
    features.addAll("-gitci")
    outputFormats = listOf("html", "xml")
    failWhenNoMutations = true
  }

  jvmArgs.add("--add-opens=java.base/java.lang=ALL-UNNAMED")
  jvmArgs.add("-Dorg.gradle.testkit.debug=true")
}

val pitestReportLocation: Provider<Directory> = project.layout.buildDirectory.dir("reports/pitest")

val printPitestReportLocation by tasks.registering {
  val location = pitestReportLocation.map { it.file("index.html") }
  doLast {
    println("Pitest report: file://${location.get()}")
  }
}

tasks.named("pitest").configure {
  finalizedBy(printPitestReportLocation)
}
