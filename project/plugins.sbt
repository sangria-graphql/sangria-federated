addSbtPlugin("com.github.sbt" % "sbt-github-actions" % "0.25.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.5")
addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")

// https://github.com/scalapb/ScalaPB
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.20"
