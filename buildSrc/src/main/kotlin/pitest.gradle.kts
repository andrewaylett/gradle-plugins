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

@file:Suppress("UnstableApiUsage")

plugins {
  `jvm-test-suite`
  id("testing")
  id("info.solidsoft.pitest")
  id("com.groupcdg.pitest.github")
}

dependencies {
  pitest("org.slf4j:slf4j-api:2.0.9")
  pitest("ch.qos.logback:logback-classic:1.4.11")
  pitest("com.groupcdg.arcmutate:base:1.2.2")
  pitest("com.groupcdg.pitest:pitest-accelerator-junit5:1.0.6")
  pitest("com.groupcdg:pitest-git-plugin:1.1.2")
  pitest("com.groupcdg.pitest:pitest-kotlin-plugin:1.1.5")
}

inline fun <T, R> Iterable<T>.transform(crossinline transform: (T) -> R): Iterable<R> {
  return object: Iterable<R> {
    override fun iterator(): Iterator<R> {
      val wrapped = this@transform.iterator()
      return object: Iterator<R> {
        override fun hasNext(): Boolean = wrapped.hasNext()

        override fun next(): R = transform.invoke(wrapped.next())
      }
    }
  }
}

val isCI = providers.environmentVariable("CI").isPresent
pitest {
  targetClasses.add("eu.aylett.*")
  testSourceSets.set(testing.suites.withType<JvmTestSuite>().transform { it.sources })
  // Don't mutate the class that calls git
  excludedClasses.add("eu.aylett.gradle.gitversion.Git")

  junit5PluginVersion = "1.2.1"
  verbosity = "VERBOSE_NO_SPINNER"
  pitestVersion = "1.15.1"
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
