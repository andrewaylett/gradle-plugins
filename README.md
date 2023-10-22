# Pitest fails on green test run

This branch shows a case where running `./gradlew check` succeeds, but running
`./gradlew pitest` fails complaining that tests are failing.

We have two identical tests in
[JvmConventionTest.kt](src/test/kotlin/eu/aylett/gradle/plugins/conventions/JvmConventionTest.kt),
but there could be more.  The first test run by a minion that calls into
ProjectBuilder.build() fails, and all subsequent texts pass.

The contents of the `main` source set is purely to allow pitest to
