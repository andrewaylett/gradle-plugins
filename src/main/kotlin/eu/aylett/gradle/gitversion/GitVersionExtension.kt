package eu.aylett.gradle.gitversion

import groovy.lang.Closure
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

abstract class GitVersionExtension(
  private val serviceProvider: Provider<GitVersionCacheService>,
  @Inject private val layout: ProjectLayout,
) {
  abstract val isolateGit: Property<Boolean>

  val gitVersion: Closure<String>
    get() =
      object : Closure<String>(this, this) {
        fun doCall(): String =
          serviceProvider.get().getGitVersion(layout.projectDirectory.asFile.toPath(), "")

        fun doCall(prefix: Provider<String>): String =
          serviceProvider.get().getGitVersion(layout.projectDirectory.asFile.toPath(), prefix)

        fun doCall(prefix: Any?): String =
          serviceProvider.get().getGitVersion(layout.projectDirectory.asFile.toPath(), prefix)
      }
  val versionDetails: Closure<VersionDetails>
    get() =
      object : Closure<VersionDetails>(this, this) {
        fun doCall(): VersionDetails =
          serviceProvider.get().getVersionDetails(layout.projectDirectory.asFile.toPath(), "")

        fun doCall(prefix: Provider<String>): VersionDetails =
          serviceProvider.get().getVersionDetails(layout.projectDirectory.asFile.toPath(), prefix)

        fun doCall(prefix: Any?): VersionDetails =
          serviceProvider.get().getVersionDetails(layout.projectDirectory.asFile.toPath(), prefix)
      }
}
