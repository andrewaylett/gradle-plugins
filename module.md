# Module gradle-plugins

This project provides Gradle Plugins and Conventions for building JVM projects.

## Getting Started

Add the [main conventions plugin](https://gradle-plugins.aylett.eu/gradle-plugins/eu.aylett.gradle.plugins.conventions/index.html) to your build's plugins block:

```kotlin
plugins {
  id("eu.aylett.conventions") version "0.1.0"
}
```

## Dependency Locking

This module ships a small plugin to help you generate and enforce a lock file of dependency versions across your build.

- Plugin ID: `eu.aylett.lock-dependencies`
- Task: `writeVersionsLocks` writes a `versions.lock` file at the root of the build
- File format: each non-comment line is `group:artifact:version (…reason…)`, blank lines are allowed to reduce merge conflicts

### Multi-project builds with a platform project

In multi-project builds, the preferred workflow is to have a dedicated platform subproject that defines and enforces versions for the rest of the build.

1) In all your non-platform projects, apply the plugin and declare a dependency on the platform project.  The easiest way to do this is using a convention plugin in `buildSrc`.

    build.gradle (Groovy DSL):

    ```groovy
    plugins {
        id 'java'
        id 'eu.aylett.lock-dependencies' version '0.1.0'
    }

    dependencies {
        implementation(platform(project(":platform")))
    }
    ```

2) Create a `platform` subproject configured as a Java Platform and apply the lock plugin there.

    platform/build.gradle (Groovy DSL):

    ```groovy
    plugins {
        id 'java-platform'
        id 'eu.aylett.lock-dependencies' version '0.1.0'
    }
    ```

Notes:
- On Java Platform projects, `writeVersionsLocks` is automatically enabled by the plugin. Running `./gradlew writeVersionsLocks` will generate/update the shared `versions.lock` file at the root of the build.
- The plugin coordinates dependencies from other subprojects so the generated lock reflects the full set of external modules used across the build.
 - For this reason, we use an unscoped call to the `writeVersionsLocks` task.  This ensures that every project is configured, so the service can ensure we collect dependencies from every project.
- The platform project reads `versions.lock` and converts each entry into a constraint on its `api` configuration. Consumer projects then pull in those constraints via `implementation(platform(project(":platform")))`.

### Updating dependencies

You have two common ways to update versions:

- Regenerate to latest resolved versions: simply run `./gradlew writeVersionsLocks`. The task resolves the current dependency graph and rewrites `versions.lock` entries accordingly.

- Pin or bump a version explicitly: edit `versions.lock` by changing the right-hand version in the `group:artifact:version` triplet.  The note on reasons will be added if you re-lock.

I recommend running `./gradlew writeVersionsLocks` from [pre-commit](https://pre-commit.com), to ensure it reflects the current state of the build.
I recommend _not_ making the `check` task depend on `writeVersionsLocks`, or running it in CI unless you check that the resulting file is unchanged.

Tips:
- The write task is disabled by default on non-platform projects, you should not need to enable it if you're using a platform project.
- The lock file lives at the root of the build (`versions.lock`) and is intended to be checked into version control.

### Single project builds

For a single project you can apply the plugin directly and run the write task when you want to (re)generate the lock file.

This will be most useful if you're using a tool like Renovate to bump the versions in your lockfile.

If you want to ensure that your dependencies resolve consistently, you should use [Gradle's own tooling](https://docs.gradle.org/current/userguide/dependency_resolution_consistency.html) for that, as shown below.

build.gradle (Groovy DSL):

```groovy
plugins {
    id 'java'
    id 'eu.aylett.lock-dependencies' version '0.1.0'
}

repositories {
    mavenCentral()
}

// To allow Renovate (or similar) to bump transitive dependencies
versionsLocks {
    enableLockWriting()
    extendAllConfigurations()
}

// Ensure consistent resolution of versions across configurations
java {
    consistentResolution {
        useCompileClasspathVersions()
    }
}
```

Then run:

```
./gradlew writeVersionsLocks
```

This will create a `versions.lock` file in the root project.  Non-empty lines not starting with `#` contain the resolved coordinates of all external dependencies on your classpath.
