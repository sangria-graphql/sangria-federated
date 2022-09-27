addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.13.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")

// https://github.com/scalapb/ScalaPB
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.2")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.11"
