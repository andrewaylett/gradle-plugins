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

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.net.URI

plugins {
  id("eu.aylett.conventions") version "0.1.0"
  `java-gradle-plugin`
  id("org.jetbrains.kotlin.jvm") version "1.9.10"
  `kotlin-dsl`
  id("com.diffplug.spotless") version "6.22.0"
  id("org.jetbrains.dokka") version "1.9.10"
  id("com.gradle.plugin-publish") version "1.2.1"
  id("info.solidsoft.pitest") version "1.15.0"
  id("com.groupcdg.pitest.github") version "1.0.4"
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
  pitest("com.groupcdg.arcmutate:base:1.2.2")
  pitest("com.groupcdg.pitest:pitest-accelerator-junit5:1.0.6")
  pitest("com.groupcdg:pitest-git-plugin:1.1.2")
  pitest("com.groupcdg.pitest:pitest-kotlin-plugin:1.1.3")
}

aylett {
  jvm {
    jvmVersion.set(17)
  }
}

val check = tasks.named("check")
testing {
  suites {
    register("functionalTest", JvmTestSuite::class) {
      targets.configureEach {
        testTask.configure {
          shouldRunAfter(tasks.named("test"))
        }
        check.configure {
          dependsOn(testTask)
        }
      }
    }

    withType(JvmTestSuite::class).configureEach {
      useJUnitJupiter()
      dependencies {
        implementation("org.hamcrest:hamcrest:2.2")
        implementation(gradleApi())
        implementation(gradleTestKit())
      }
    }
  }
}

spotless {
  kotlin {
    ktlint()
  }
  kotlinGradle {
    ktlint()
  }
}

val spotlessApply = tasks.named("spotlessApply")
val spotlessCheck = tasks.named("spotlessCheck")
check.configure { dependsOn(spotlessCheck) }
spotlessApply.configure { mustRunAfter(tasks.named("clean")) }

if (!providers.environmentVariable("CI").isPresent) {
  spotlessCheck.configure { dependsOn(spotlessApply) }
}

val spotlessKotlinApply = tasks.named("spotlessKotlinApply")
val spotlessKotlinCheck = tasks.named("spotlessKotlinCheck")

tasks.withType<KotlinCompilationTask<KotlinCommonCompilerOptions>>().configureEach {
  mustRunAfter(spotlessKotlinApply)
  shouldRunAfter(spotlessKotlinCheck)
}

pitest {
  junit5PluginVersion.set("1.2.0")
  verbosity.set("VERBOSE")
  pitestVersion.set("1.15.1")
  failWhenNoMutations.set(false)
  timeoutFactor.set(BigDecimal.TEN)
  outputFormats.set(listOf("html", "gitci"))
  features.add("+auto_threads")
  if (providers.environmentVariable("REPO_TOKEN").isPresent) {
    // Running in GitHub Actions
    features.addAll("+git(from[HEAD~1])", "+gitci(level[error])")
  }
  jvmArgs.add("--add-opens=java.base/java.lang=ALL-UNNAMED")
  mutators.set(listOf("STRONGER", "EXTENDED"))
}

tasks.withType<DokkaTask>().configureEach {
  dokkaSourceSets {
    configureEach {
      includes.from(projectDir.resolve("module.md"))
      jdkVersion.set(17)

      sourceLink {
        localDirectory.set(projectDir.resolve("src"))
        remoteUrl.set(URI("https://github.com/andrewaylett/gradle-plugins/tree/main/src").toURL())
        remoteLineSuffix.set("#L")
        externalDocumentationLink {
          url.set(URI("https://docs.gradle.org/8.4/javadoc/").toURL())
        }
      }
    }
  }
}

group = "eu.aylett"

gradlePlugin {
  website = "https://gradle-plugins.aylett.eu/"
  vcsUrl = "https://github.com/andrewaylett/gradle-plugins"

  plugins {
    create("basePlugin") {
      id = "eu.aylett.plugins.base"
      displayName = "aylett.eu base plugin"
      description = "Base plugin for registering common Gradle features"
      tags = listOf("base", "jvm")
      //language=jvm-class-name
      implementationClass = "eu.aylett.gradle.plugins.BasePlugin"
    }
    create("bomAlignmentConvention") {
      id = "eu.aylett.conventions.bom-alignment"
      displayName = "aylett.eu BOM alignment plugin"
      description = "Adds virtual BOM for common sets of packages that don't have a real BOM"
      tags = listOf("bom", "jvm")
      //language=jvm-class-name
      implementationClass = "eu.aylett.gradle.plugins.conventions.BomAlignmentConvention"
    }
    create("ideSupportConvention") {
      id = "eu.aylett.conventions.ide-support"
      displayName = "aylett.eu IDE support conventions"
      description = "Conventional support for JetBrains IDEs"
      tags = listOf("ide", "idea", "conventions", "jvm")
      //language=jvm-class-name
      implementationClass = "eu.aylett.gradle.plugins.conventions.IDESupportConvention"
    }
    create("jvmConvention") {
      id = "eu.aylett.conventions.jvm"
      displayName = "aylett.eu JVM conventions"
      description = "Conventional support for JVM build features, like integration tests"
      tags = listOf("jvm", "testing", "integrationtests", "conventions")
      //language=jvm-class-name
      implementationClass = "eu.aylett.gradle.plugins.conventions.JvmConvention"
    }
    create("allConventions") {
      id = "eu.aylett.conventions"
      displayName = "aylett.eu conventions"
      description = "Applies all Andrew's favourite build conventions"
      tags = listOf("conventions", "jvm")
      //language=jvm-class-name
      implementationClass = "eu.aylett.gradle.plugins.conventions.Conventions"
    }
  }
}
