package eu.aylett.gradle.extensions

import org.gradle.api.provider.Property

interface JvmConventionExtension {
  val jvmVersion: Property<Int>
}
