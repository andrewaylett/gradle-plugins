# Andrew's Gradle Conventions

This is a set of reasonably-opinionated conventions for developing JVM projects
using Gradle. They're my opinion of current best practice, both for writing
plugins and for the set up of projects.

In an ideal world, absent any requirement to publish the build output, you need
only apply my conventions plugin and you'll have a magically wonderful build
experience.

## Applying the plugins

If you're sure you agree with all my decisions, you may import the main
conventions file into your `build.gradle.kts` or `build.gradle`.

If you have a multi-project build, I recommend adding a `buildSrc` project with
a plugin that configures the correct version of this plugin (along with and
customisation, or extra configuration) then applying _that_ plugin to each of
your projects.

If you have multiple builds, and you want different conventions from me, you
should probably do what I've done and publish your own conventions. My hope is
that this project might be a useful base for people to start with.
