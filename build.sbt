maintainer := "simple-hosting@simplehosting.org"

name         := """simple-hosting.web2"""
organization := "com.simplehosting"

version := "12.09.23.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.3.0"

libraryDependencies += guice

libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.0-M6" % Test

libraryDependencies += "org.apache.httpcomponents.client5" % "httpclient5"          % "5.2.1"
libraryDependencies += "com.fasterxml.jackson.module"     %% "jackson-module-scala" % "2.14.1"
libraryDependencies += "io.github.heavypunk" %% "simple-hosting.compositor.client" % "20.04.23.1" from "https://github.com/HeavyPunk/simple-hosting.compositor.client/raw/main/build/simplehosting-compositor-client-20.04.23.1.jar"
libraryDependencies += "io.github.heavypunk" %% "simple-hosting.controller.client" % "06.14.23.1" from "https://github.com/HeavyPunk/simple-hosting.controller.client/raw/master/build/simple-hosting-controller-client_2.13-01.22.24.1.jar"

// Slick
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.5.0-M4",
  "org.slf4j" % "slf4j-nop" % "1.7.26",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.5.0-M4",
  "org.postgresql" % "postgresql" % "42.5.1",
)

// S3
libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.12.609";

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.simplehosting.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.simplehosting.binders._"
