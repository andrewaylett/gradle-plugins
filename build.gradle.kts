@file:Suppress("UnstableApiUsage")

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    idea
    id("com.diffplug.spotless") version "6.22.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
}

javaToolchains {
    compilerFor {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    target {
        jvmToolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }
}

testing {
    suites {
        register("functionalTest", JvmTestSuite::class) {
            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(tasks.named("test"))
                }
                tasks.named("check").configure {
                    dependsOn(testTask)
                }
            }
        }

        withType(JvmTestSuite::class).configureEach {
            useJUnitJupiter()
            dependencies {
                implementation("org.assertj:assertj-core:3.24.2")
                implementation(gradleApi())
                implementation(gradleTestKit())
            }
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
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

tasks.named("check").configure { dependsOn(tasks.named("spotlessCheck")) }
tasks.named("spotlessApply").configure { mustRunAfter(tasks.named("clean")) }

if (!providers.environmentVariable("CI").isPresent) {
    tasks.named("spotlessCheck").configure { dependsOn(tasks.named("spotlessApply")) }
}

tasks.named("compileKotlin").configure { mustRunAfter(tasks.named("spotlessKotlinApply")) }
tasks.named("compileTestKotlin").configure { mustRunAfter(tasks.named("spotlessKotlinApply")) }
tasks.named("compileFunctionalTestKotlin").configure {
    mustRunAfter(tasks.named("spotlessKotlinApply"))
}

group = "eu.aylett"
version = "0.1.0"

gradlePlugin {
    website = "https://aylett.eu/gradle-plugins"
    vcsUrl = "https://github.com/andrewaylett/gradle-plugins"

    plugins {
        create("basePlugin") {
            id = "eu.aylett.plugins.base"
            displayName = "aylett.eu base plugin"
            description = "Base plugin for registering common Gradle features"
            tags = listOf()
            //language=jvm-class-name
            implementationClass = "eu.aylett.gradle.plugins.BasePlugin"
        }
        create("bomAlignmentConvention") {
            id = "eu.aylett.conventions.bom-alignment"
            displayName = "aylett.eu BOM alignment plugin"
            description = "Adds virtual BOM for common sets of packages that don't have a real BOM"
            tags = listOf("bom")
            //language=jvm-class-name
            implementationClass = "eu.aylett.gradle.plugins.conventions.BomAlignmentConvention"
        }
        create("ideSupportConvention") {
            id = "eu.aylett.conventions.ide-support"
            displayName = "aylett.eu IDE support conventions"
            description = "Conventional support for JetBrains IDEs"
            tags = listOf("ide", "idea", "conventions")
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
            tags = listOf("conventions")
            //language=jvm-class-name
            implementationClass = "eu.aylett.gradle.plugins.conventions.Conventions"
        }
    }
}
