@file:Suppress("UnstableApiUsage")

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

plugins {
  `java-gradle-plugin`
  id("org.jetbrains.kotlin.jvm") version "1.9.10"
  `kotlin-dsl`
  id("info.solidsoft.pitest") version "1.15.0"
}

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
}

val check = tasks.named("check")
testing {
  suites {
    withType(JvmTestSuite::class).configureEach {
      useJUnitJupiter()
      dependencies {
        implementation("org.hamcrest:hamcrest:2.2")
        implementation(gradleApi())
      }
    }
  }
}

pitest {
  junit5PluginVersion.set("1.2.0")
  verbosity.set("VERBOSE")
  avoidCallsTo.set(listOf("kotlin.jvm.internal"))
  pitestVersion.set("1.15.1")
  verbose.set(true)
  threads.set(1)
  targetClasses.set(listOf("eu.aylett.*"))
}
