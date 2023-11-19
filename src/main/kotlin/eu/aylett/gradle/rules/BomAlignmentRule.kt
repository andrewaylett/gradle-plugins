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

package eu.aylett.gradle.rules

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

/**
 * Make sure we use the same version of all dependencies that are known to not specify Gradle Module
 * Metadata. Applied to projects using the [eu.aylett.gradle.plugins.conventions.BomAlignmentConvention] plugin.
 */
internal class BomAlignmentRule : ComponentMetadataRule {
  override fun execute(ctx: ComponentMetadataContext) {
    val details = ctx.details
    val group = details.id.group
    val name = details.id.name
    val version = details.id.version
    if (group == "org.mockito") {
      details.belongsTo("org.mockito:mockito-virtual-bom:$version")
    } else if (group == "io.dropwizard.metrics") {
      details.belongsTo("io.dropwizard.metrics:metrics-virtual-bom:$version")
    } else if (group == "com.google.api.grpc" && name.endsWith("-google-common-protos")) {
      details.belongsTo("com.google.api.grpc:google-common-protos-virtual-bom:$version")
    } else if (group == "io.dropwizard" && name == "dropwizard-util") {
      // All Dropwizard packages, it seems, depend on dropwizard-util.  Use it to pull in the BOM.
      details.belongsTo("io.dropwizard:dropwizard-bom:$version", false)
    } else if (group == "org.glassfish.jersey.core" && name == "jersey-common") {
      // All Jersey packages, it seems, depend on jersey-common.  Use it to pull in the BOM.
      details.belongsTo("org.glassfish.jersey:jersey-bom:$version", false)
    } else if (group == "org.jetbrains.kotlin" && name.startsWith("kotlin-stdlib")) {
      // The Kotlin BOM supports many but not all of the objects in the group.
      // But we can pull the BOM in based on stdlib being present
      details.belongsTo("org.jetbrains.kotlin:kotlin-bom:$version", false)
    } else if (group == "io.grpc" && name == "grpc-api") {
      // All GRPC packages, it seems, depend on grpc-api.  Use it to pull in the BOM.
      details.belongsTo("io.grpc:grpc-bom:$version", false)
    } else if (group == "org.assertj") {
      details.belongsTo("org.assertj:assertj-virtual-bom:$version")
    } else if (group == "com.google.protobuf" && name == "protobuf-java") {
      // All protobuf packages, it seems, depend on protobuf-java.  Use it to pull in the BOM.
      details.belongsTo("com.google.protobuf:protobuf-bom:$version", false)
    }
  }
}
