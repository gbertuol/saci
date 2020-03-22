lazy val commonSettings = Seq(
  organization   := "org.bertuol",
  headerLicense  := Some(HeaderLicense.MIT("2019", "Guilherme Bertuol")),
  headerMappings := headerMappings.value + (HeaderFileType.scala -> HeaderCommentStyle.cppStyleLineComment),
  scalaVersion   := "2.13.1",
  // ---------------------------------------------------------------------------
  // Options for testing
  testFrameworks += new TestFramework("minitest.runner.Framework"),
  logBuffered in Test            := false,
  logBuffered in IntegrationTest := false,
  // Disables parallel execution
  parallelExecution in Test             := false,
  parallelExecution in IntegrationTest  := false,
  testForkedParallel in Test            := false,
  testForkedParallel in IntegrationTest := false,
  concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
  // ---------------------------------------------------------------------------
  // Common deps
  libraryDependencies ++= Seq(
    "org.typelevel" %% "simulacrum"    % "1.0.0" % Provided,
    "org.typelevel" %% "cats-core"     % "2.1.0",
    "org.typelevel" %% "cats-effect"   % "2.1.1",
    "io.circe" %% "circe-core"         % "0.12.3",
    "co.fs2" %% "fs2-core"             % "2.2.1",
    "co.fs2" %% "fs2-reactive-streams" % "2.2.1"
  )
)

lazy val saci = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .dependsOn(core, pg, tests)
  .aggregate(core, pg, tests)

lazy val core = project
  .in(file("modules/core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    name := "saci-core"
  )

lazy val pg = project
  .in(file("modules/pg"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    name := "saci-pg",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "skunk-core"  % "0.0.7",
      "org.tpolecat" %% "skunk-circe" % "0.0.7"
    )
  )
  .dependsOn(core)

lazy val tests = project
  .in(file("modules/tests"))
  .dependsOn(core, pg)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.codecommit" %% "cats-effect-testing-specs2" % "0.4.0" % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
