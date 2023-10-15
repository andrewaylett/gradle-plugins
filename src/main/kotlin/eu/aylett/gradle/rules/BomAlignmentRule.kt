package eu.aylett.gradle.rules

import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

/**
 * Make sure we use the same version of all dependencies that are known to not specify Gradle Module
 * Metadata. Applied to projects using the [eu.aylett.gradle.plugins.conventions.BomAlignmentConvention] plugin.
 */
class BomAlignmentRule : ComponentMetadataRule {
  override fun execute(ctx: ComponentMetadataContext) {
    val details = ctx.details
    val id = details.id
    val group = id.group
    if (group.startsWith("org.mockito")) {
      details.belongsTo("org.mockito:mockito-virtual-bom:${id.version}")
    } else if (group.startsWith("io.dropwizard.metrics")) {
      details.belongsTo("io.dropwizard.metrics:metrics-virtual-bom:${id.version}")
    } else if (group == "io.dropwizard") {
      details.belongsTo("io.dropwizard:dropwizard-virtual-bom:${id.version}")
    } else if (group.startsWith("org.glassfish.jersey")) {
      details.belongsTo("org.glassfish.jersey:jersey-virtual-bom:${id.version}")
    } else if (group.startsWith("org.jetbrains.kotlin")) {
      details.belongsTo("org.jetbrains.kotlin:kotlin-virtual-bom:${id.version}")
    } else if (group == "org.assertj") {
      details.belongsTo("org.assertj:assertj-virtual-bom:${id.version}")
    } else if (group == "com.google.protobuf") {
      details.belongsTo("com.google.protobuf:proto-virtual-bom:${id.version}")
    }
  }
}
