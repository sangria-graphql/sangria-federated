name := "sangria-federated"
organization := "org.sangria-graphql"

description := "Sangria federated"
homepage := Some(url("http://sangria-graphql.org"))
licenses := Seq(
  "Apache License, ASL Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

ThisBuild / crossScalaVersions := Seq("2.12.13", "2.13.4")
ThisBuild / scalaVersion := crossScalaVersions.value.last
ThisBuild / githubWorkflowPublishTargetBranches := List()
ThisBuild / githubWorkflowBuildPreamble ++= List(
  WorkflowStep.Sbt(List("scalafmtCheckAll"), name = Some("Check formatting"))
)

scalacOptions ++= Seq("-deprecation", "-feature")

scalacOptions += "-target:jvm-1.8"
javacOptions ++= Seq("-source", "8", "-target", "8")

libraryDependencies ++= Seq(
  "org.sangria-graphql" %% "sangria" % "2.1.0",
  "org.scalatest" %% "scalatest" % "3.2.5" % Test,
  "io.circe" %% "circe-generic" % "0.13.0" % Test,
  "io.circe" %% "circe-parser" % "0.13.0" % Test,
  "org.sangria-graphql" %% "sangria-circe" % "1.3.1" % Test,
)

// Publishing
releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := (_ => false)
publishTo := Some(
  if (version.value.trim.endsWith("SNAPSHOT"))
    "snapshots".at("https://oss.sonatype.org/content/repositories/snapshots")
  else
    "releases".at("https://oss.sonatype.org/service/local/staging/deploy/maven2"))

startYear := Some(2021)
organizationHomepage := Some(url("https://github.com/sangria-graphql"))
developers := List(
  Developer("xsoufiane", "Soufiane Maguerra", "", url("https://github.com/xsoufiane")),
  Developer("yanns", "Yann Simon", "", url("https://github.com/yanns")))
scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/sangria-graphql/sangria-federated.git"),
    connection = "scm:git:git@github.com:sangria-graphql/sangria-federated.git"
  ))
