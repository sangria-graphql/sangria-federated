addSbtPlugin("com.github.sbt" % "sbt-github-actions" % "0.32.1")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")

// https://github.com/scalapb/ScalaPB
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.1.0-RC2")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "1.0.0-alpha.6"
