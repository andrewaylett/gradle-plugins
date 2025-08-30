# Andrew's Gradle Conventions

This is a set of reasonably-opinionated conventions for developing JVM projects
using Gradle. They're my opinion of current best practice, both for writing
plugins and for the set up of projects.

In an ideal world, absent any requirement to publish the build output, you need
only apply my conventions plugin and you'll have a magically wonderful build
experience.

## Documentation

[Documentation](https://gradle-plugins.aylett.eu) is automatically generated
from the source code and [module.md](module.md)

## Applying the plugins

If you're sure you agree with all my decisions, you may import the main
conventions plugin into your `plugins` block:

Gradle Kotlin DSL:

```kotlin
plugins {
  id("eu.aylett.conventions") version "<latest>"
}
```

Groovy DSL:

```groovy
plugins {
  id 'eu.aylett.conventions' version '<latest>'
}
```

Common plugin IDs available from this project:
- eu.aylett.conventions — apply all conventions.
- eu.aylett.conventions.jvm — JVM build conventions (testing, integration tests, etc.).
- eu.aylett.conventions.ide-support — IDE integration helpers.
- eu.aylett.conventions.bom-alignment — virtual BOM alignment.
- eu.aylett.plugins.version — sets version from the Git repository state.
- eu.aylett.lock-dependencies — generate and enforce a versions.lock across the build.

If you have a multi-project build, I recommend adding a `buildSrc` project with
a plugin that pins the desired versions of these plugins (along with any
customisation or extra configuration) and then applying that plugin to each of
your projects.

If you have multiple builds and want different conventions to mine, you should
probably do what I've done and publish your own conventions. My hope is that
this project might be a useful base for people to start with.

### Dependency locking quickstart

This repository ships a small plugin to help generate and enforce a lock file of
dependency versions across your build.

Single project (Groovy DSL):

```groovy
plugins {
  id 'java'
  id 'eu.aylett.lock-dependencies' version '<latest>'
}

repositories {
  mavenCentral()
}

versionsLocks {
    enableLockWriting()
    extendAllConfigurations()
}
```

Then run:

```
./gradlew writeVersionsLocks
```

This creates a `versions.lock` file at the root of the build. For a recommended
multi-project setup with a platform project, see module.md.

The format of the `versions.lock` file matches that used by Palantir's [gradle-consistent-versions](https://github.com/palantir/gradle-consistent-versions) plugin, but the implementation here is entirely new.
The intent is that we can use tools (like Renovate) that understand the lock file format, but with code that's compatible with Gradle's isolated projects features.

## Contributing and development

- Build: `./gradlew build`
- Run unit and functional tests: `./gradlew check`
- Generate local documentation site: `./gradlew dokkaHtml` and open `build/dokka/html/index.html`

This project is licensed under the Apache License 2.0, see LICENSE.
