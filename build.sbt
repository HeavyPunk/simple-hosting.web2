maintainer := "simple-hosting@simplehosting.org"

name         := """simple-hosting.web2"""
organization := "com.simplehosting"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.10"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies += "org.apache.httpcomponents.client5" % "httpclient5"          % "5.2.1"
libraryDependencies += "com.google.code.gson"              % "gson"                 % "2.10.1"
libraryDependencies += "com.fasterxml.jackson.module"     %% "jackson-module-scala" % "2.14.1"
libraryDependencies += "io.github.heavypunk" %% "simple-hosting.compositor.client" % "1.0.1" from "https://github.com/HeavyPunk/simple-hosting.compositor.client/raw/main/build/simple-hosting-compositor-client.jar"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.simplehosting.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.simplehosting.binders._"
