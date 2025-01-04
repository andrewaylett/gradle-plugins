package eu.aylett.gradle.gitversion

import groovy.lang.Closure
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class GitVersionExtension(
  private val serviceProvider: Provider<GitVersionCacheService>,
  @Inject private val layout: ProjectLayout,
  @Inject private val providers: ProviderFactory,
) {
  val gitVersion: Closure<String>
    get() =
      object : Closure<String>(this, this) {
        fun doCall(): String =
          serviceProvider.get().getGitVersion(
            layout.projectDirectory.asFile.toPath(),
            "",
            providers,
          )

        fun doCall(prefix: Provider<String>): String =
          serviceProvider.get().getGitVersion(
            layout.projectDirectory.asFile.toPath(),
            prefix,
            providers,
          )

        fun doCall(prefix: Any?): String =
          serviceProvider.get().getGitVersion(
            layout.projectDirectory.asFile.toPath(),
            prefix,
            providers,
          )
      }
  val versionDetails: Closure<VersionDetails>
    get() =
      object : Closure<VersionDetails>(this, this) {
        fun doCall(): VersionDetails =
          serviceProvider.get().getVersionDetails(
            layout.projectDirectory.asFile.toPath(),
            "",
            providers,
          )

        fun doCall(prefix: Provider<String>): VersionDetails =
          serviceProvider.get().getVersionDetails(
            layout.projectDirectory.asFile.toPath(),
            prefix,
            providers,
          )

        fun doCall(prefix: Any?): VersionDetails =
          serviceProvider.get().getVersionDetails(
            layout.projectDirectory.asFile.toPath(),
            prefix,
            providers,
          )
      }
}
