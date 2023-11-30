package eu.aylett.gradle.functionaltests.gitversion

import eu.aylett.gradle.gitversion.NativeGit
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers.containsInRelativeOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.Test
import java.util.regex.Pattern
import kotlin.io.path.appendText
import kotlin.io.path.writeText

private val DETACHED_HEAD_MODE_REGEX: Pattern =
  Pattern.compile(
    "[a-z0-9]{10}",
  )

class GitVersionPluginKotlinTests : GitVersionPluginTests(true) {
  @Test
  fun `isCleanTag should be false when repo dirty on a tag checkout`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id("eu.aylett.plugins.version")
      }
      version = aylett.versions.gitVersion()
      tasks.register("printVersionDetails").configure {
        doLast {
          println(aylett.versions.versionDetails().isCleanTag)
        }
      }

      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = NativeGit(projectDir)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    dirtyContentFile.writeText("dirty-content")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    MatcherAssert.assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder("false"),
    )
  }

  @Test
  fun `version details when detached HEAD mode`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id("eu.aylett.plugins.version")
      }
      version = aylett.versions.gitVersion()
      tasks.register("printVersionDetails").configure {
        doLast {
          println(aylett.versions.versionDetails().lastTag)
          println(aylett.versions.versionDetails().commitDistance)
          println(aylett.versions.versionDetails().gitHash)
          println(aylett.versions.versionDetails().branchName)
        }
      }

      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = NativeGit(projectDir)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    val commitId = git.currentHeadFullHash
    git.runGitCommand("tag", "-a", "1.0.0", "-m", "1.0.0")
    git.runGitCommand("commit", "-m", "commit 2", "--allow-empty")
    git.runGitCommand("checkout", commitId)

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    MatcherAssert.assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder(
        equalTo("1.0.0"),
        equalTo("0"),
        matchesRegex(DETACHED_HEAD_MODE_REGEX),
        equalTo(""),
      ),
    )
  }

  @Test
  fun `version filters out tags not matching prefix and strips prefix`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id("eu.aylett.plugins.version")
      }
      version = aylett.versions.gitVersion()
      tasks.register("printVersionDetails").configure {
        doLast {
          println(aylett.versions.versionDetails("my-product@").lastTag)
        }
      }
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = NativeGit(projectDir)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "my-product@1.0.0", "-m", "my-product@1.0.0")
    git.runGitCommand("commit", "-m", "commit 2", "--allow-empty")
    git.runGitCommand("tag", "-a", "2.0.0", "-m", "2.0.0")

    // when:
    val buildResult = with("printVersionDetails").build()

    // then:
    MatcherAssert.assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder(equalTo("1.0.0")),
    )
  }

  @Test
  fun `git describe with commit after annotated tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id("eu.aylett.plugins.version")
      }
      version = aylett.versions.gitVersion()
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = NativeGit(projectDir)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "-a", "v1.0.0", "-m", "1.0.0")
    dirtyContentFile.writeText("dirty-content")
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "add some stuff")
    val commitSha = git.currentHeadFullHash

    // when:
    val buildResult = with("printVersion").build()

    // then:
    MatcherAssert.assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder("1.0.0-1-g${commitSha.substring(0, 7)}"),
    )
  }

  @Test
  fun `git describe with commit after lightweight tag`() {
    // given:
    buildFile.writeText(
      """
      plugins {
        id("eu.aylett.plugins.version")
      }
      version = aylett.versions.gitVersion()
      """.trimIndent(),
    )
    gitIgnoreFile.appendText("build")
    val git = NativeGit(projectDir)
    git.runGitCommand("init", projectDir.toString())
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "initial commit")
    git.runGitCommand("tag", "v1.0.0")
    dirtyContentFile.writeText("dirty-content")
    git.runGitCommand("add", ".")
    git.runGitCommand("commit", "-m", "add some stuff")
    val commitSha = git.currentHeadFullHash

    // when:
    val buildResult = with("printVersion").build()

    // then:
    MatcherAssert.assertThat(
      buildResult.output.split('\n'),
      containsInRelativeOrder("1.0.0-1-g${commitSha.substring(0, 7)}"),
    )
  }
}
