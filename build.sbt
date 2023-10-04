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
        browseUrl = url("https://github.com/sangria-graphql/sangria-federated"),
        connection = "scm:git:git@github.com:sangria-graphql/sangria-federated.git"
      ))
  )
)

// Build
ThisBuild / crossScalaVersions := Seq("2.12.18", "2.13.12")
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

lazy val root = (project in file("."))
  .settings(
    name := "sangria-federated",
    description := "Federation for Sangria"
  )
  .settings(noPublishSettings)
  .aggregate(core, exampleCommon, exampleReview, exampleState, exampleTest)

lazy val core = libraryProject("core")
  .settings(
    name := "sangria-federated",
    description := "Sangria federated",
    libraryDependencies ++= Seq(
      Dependencies.sangria,
      Dependencies.scalapbRuntime
    ),
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= Seq(
      Dependencies.scalaTest,
      Dependencies.circeGeneric,
      Dependencies.circeParser,
      Dependencies.sangriaCirce
    ).map(_ % Test)
  )

lazy val exampleReview = exampleProject("example-review")
  .dependsOn(exampleCommon)
  .settings(libraryDependencies ++= serviceDependencies)

lazy val exampleState = exampleProject("example-state")
  .dependsOn(exampleCommon)
  .settings(libraryDependencies ++= serviceDependencies)

lazy val serviceDependencies = Seq(
  Dependencies.logbackClassic,
  Dependencies.circeGeneric
)

lazy val exampleCommon = exampleProject("example-common")
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.catsEffect,
      Dependencies.http4sEmberServer,
      Dependencies.http4sCirce,
      Dependencies.http4sDsl,
      Dependencies.circeOptics,
      Dependencies.sangria,
      Dependencies.sangriaCirce
    )
  )

lazy val exampleTest = exampleProject("example-test")
  .dependsOn(exampleReview, exampleState)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.weaver,
      Dependencies.fs2Process,
      Dependencies.nuProcess).map(_ % Test),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )

def libraryProject(name: String) = newProject(name)

def exampleProject(name: String) =
  newProject(name)
    .in(file(name.replace("example-", "example/")))
    .settings(noPublishSettings)

def newProject(name: String) =
  Project(name, file(name))
    .settings(commonSettings)

lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-deprecation", "-feature"),
  scalacOptions ++= Seq("-release", "8"),
  javacOptions ++= Seq("-source", "8", "-target", "8")
)

lazy val noPublishSettings = Seq(
  publish / skip := true
)
