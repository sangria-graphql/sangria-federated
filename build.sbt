// About
inThisBuild(
  List(
    organization := "org.sangria-graphql",
    homepage := Some(url("https://github.com/sangria-graphql/sangria-federated/")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer("xsoufiane", "Soufiane Maguerra", "", url("https://github.com/xsoufiane")),
      Developer("yanns", "Yann Simon", "", url("https://github.com/yanns"))),
    scmInfo := Some(
      ScmInfo(
        browseUrl = url("https://github.com/sangria-graphql/sangria-federated.git"),
        connection = "scm:git:git@github.com:sangria-graphql/sangria-federated.git"
      ))
  )
)

// Build
ThisBuild / crossScalaVersions := Seq("2.12.13", "2.13.5")
ThisBuild / scalaVersion := crossScalaVersions.value.last
ThisBuild / githubWorkflowBuildPreamble ++= List(
  WorkflowStep.Sbt(List("scalafmtCheckAll"), name = Some("Check formatting"))
)

// Release
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(RefPredicate.StartsWith(Ref.Tag("v")))

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

// scalacOptions, javacOptions
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature")
ThisBuild / scalacOptions += "-target:jvm-1.8"
ThisBuild / javacOptions ++= Seq("-source", "8", "-target", "8")

// sangria-federated
lazy val root = (project in file("."))
  .withId("sangria-federated")
  .settings(
    name := "sangria-federated",
    description := "Sangria federated",
    libraryDependencies ++= Seq(
      "org.sangria-graphql" %% "sangria" % "2.1.0",
      "org.scalatest" %% "scalatest" % "3.2.2" % Test,
      "io.circe" %% "circe-generic" % "0.13.0" % Test,
      "io.circe" %% "circe-parser" % "0.13.0" % Test,
      "org.sangria-graphql" %% "sangria-circe" % "1.3.1" % Test
    )
  )

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Example
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

val serviceDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.circe" %% "circe-generic" % "0.13.0"
)

// GraphQL example services
lazy val review = (project in file("example/review"))
  .dependsOn(root, common)
  .settings(
    publish / skip := true,
    libraryDependencies ++= serviceDependencies
  )

lazy val state = (project in file("example/state"))
  .dependsOn(root, common)
  .settings(
    publish / skip := true,
    libraryDependencies ++= serviceDependencies
  )

// GraphQL examples common code
lazy val common = (project in file("example/common")).settings(
  publish / skip := true,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "2.2.0",
    "org.http4s" %% "http4s-blaze-server" % "0.21.8",
    "org.http4s" %% "http4s-circe" % "0.21.8",
    "org.http4s" %% "http4s-dsl" % "0.21.8",
    "io.circe" %% "circe-optics" % "0.13.0",
    "org.sangria-graphql" %% "sangria" % "2.1.0",
    "org.sangria-graphql" %% "sangria-circe" % "1.3.1"
  )
)
