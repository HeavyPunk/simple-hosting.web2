maintainer := "simple-hosting@simplehosting.org"

name         := """simple-hosting.web2"""
organization := "com.simplehosting"

version := "05.09.23.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.10"

libraryDependencies += guice

libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1"

libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies += "org.apache.httpcomponents.client5" % "httpclient5"          % "5.2.1"
libraryDependencies += "com.fasterxml.jackson.module"     %% "jackson-module-scala" % "2.14.1"
libraryDependencies += "io.github.heavypunk" %% "simple-hosting.compositor.client" % "20.04.23.1" from "https://github.com/HeavyPunk/simple-hosting.compositor.client/raw/main/build/simplehosting-compositor-client-20.04.23.1.jar"
libraryDependencies += "io.github.heavypunk" %% "simple-hosting.controller.client" % "04.15.23.1" from "https://github.com/HeavyPunk/simple-hosting.controller.client/raw/master/build/simple-hosting-controller-client_2.13-04.15.23.1.jar"

//Hibernate dependency
libraryDependencies ++= Seq(
    "org.hibernate" % "hibernate-core" % "6.2.0.Final",
    "org.hibernate" % "hibernate-entitymanager" % "4.1.8.Final",
    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    "org.postgresql" % "postgresql" % "42.5.1",
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.simplehosting.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.simplehosting.binders._"
