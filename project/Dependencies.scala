import sbt._

object Dependencies {
  object V {
    val circe = "0.14.1"
    val http4s = "1.0.0-M29"
  }

  val sangria = "org.sangria-graphql" %% "sangria" % "3.2.0"
  val sangriaCirce = "org.sangria-graphql" %% "sangria-circe" % "1.3.2"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.11"

  val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.14"
  val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % V.http4s
  val http4sCirce = "org.http4s" %% "http4s-circe" % V.http4s
  val http4sDsl = "org.http4s" %% "http4s-dsl" % V.http4s

  val circeGeneric = "io.circe" %% "circe-generic" % V.circe
  val circeParser = "io.circe" %% "circe-parser" % V.circe
  val circeOptics = "io.circe" %% "circe-optics" % V.circe

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.13"
}
