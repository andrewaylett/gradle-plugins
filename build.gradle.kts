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

import okio.ByteString.Companion.decodeBase64


plugins {
  id("component")
  id("eu.aylett.plugins.version") version "0.5.2"
  `java-gradle-plugin`
  id("com.gradle.plugin-publish") version "1.3.0"
  `maven-publish`
  signing
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom:$embeddedKotlinVersion"))
  implementation("com.google.guava:guava:33.4.0-jre")

  testImplementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
  testImplementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
}

group = "eu.aylett"
version = aylett.versions.gitVersion()
description = "Andrew's favourite Gradle conventions"

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

  withSourcesJar()
  withJavadocJar()
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}

val checkPublishVersion by tasks.registering {
  doNotTrackState("Either does nothing or fails the build")
  doFirst {
    val versionDetails = aylett.versions.versionDetails()
    if (!versionDetails.isCleanTag) {
      logger.error("Version details is {}", versionDetails)
      throw IllegalStateException(
        "Can't publish a plugin with a version (${versionDetails.version}) that's not a clean tag",
      )
    }
  }
}
tasks.named("publishPlugins").configure {
  dependsOn(checkPublishVersion)
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/andrewaylett/gradle-plugins")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
}

nexusPublishing {
  repositories {
    sonatype {
      username = System.getenv("OSSRH_TOKEN_USER")
      password = System.getenv("OSSRH_TOKEN_PASSWORD")
    }
  }
}

publishing.publications.withType<MavenPublication>().configureEach {
  suppressPomMetadataWarningsFor("testFixturesApiElements")
  suppressPomMetadataWarningsFor("testFixturesRuntimeElements")
  pom {
    name.set("Gradle plugins: eu.aylett.gradle-plugins")
    description.set("Andrew's favourite Gradle conventions")
    url.set("https://gradle-plugins.aylett.eu/")
    licenses {
      license {
        name.set("Apache-2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0")
      }
    }
    developers {
      developer {
        id.set("aylett")
        name.set("Andrew Aylett")
        email.set("andrew@aylett.eu")
        organization.set("Andrew Aylett")
        organizationUrl.set("https://www.aylett.co.uk/")
      }
    }
    scm {
      connection.set("scm:git:https://github.com/andrewaylett/gradle-plugins.git")
      developerConnection.set("scm:git:ssh://git@github.com:andrewaylett/gradle-plugins.git")
      url.set("https://github.com/andrewaylett/gradle-plugins/")
    }
  }
}

signing {
  setRequired({
    gradle.taskGraph.hasTask(":publishPluginMavenPublicationToSonatypeHRepository")
  })
  val signingKey: String? = System.getenv("GPG_SIGNING_KEY")?.decodeBase64()?.utf8()
  useInMemoryPgpKeys(signingKey, "")
  sign(publishing.publications)
}

gradlePlugin {
  website = "https://gradle-plugins.aylett.eu/"
  vcsUrl = "https://github.com/andrewaylett/gradle-plugins"

  testSourceSets(sourceSets.getByName("functionalTest"))

  plugins {
    create("basePlugin") {
      id = "eu.aylett.plugins.base"
      displayName = "aylett.eu base plugin"
      description = "Base plugin for registering common Gradle features"
      tags = listOf("base", "jvm")
      //language=jvm-class-name
      implementationClass = "eu.aylett.gradle.plugins.BasePlugin"
    }
    create("versionPlugin") {
      id = "eu.aylett.plugins.version"
      displayName = "aylett.eu automatic version plugin"
      description = "Sets the project version from the state of the git repository it's in."
      tags = listOf("git", "version")
      //language=jvm-class-name
      implementationClass = "eu.aylett.gradle.gitversion.GitVersionPlugin"
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
