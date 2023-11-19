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

import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

plugins {
  id("eu.aylett.conventions")
  id("eu.aylett.plugins.version")
  id("testing")
  id("spotless")
  id("pitest")
  id("org.gradle.kotlin.kotlin-dsl")
  id("org.jetbrains.dokka")
  `java-library`
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:$embeddedKotlinVersion"))
}

tasks.withType<AbstractArchiveTask>().configureEach {
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

aylett {
  jvm {
    jvmVersion.set(17)
  }
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
