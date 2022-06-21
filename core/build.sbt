ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "ergos"
  )

libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"
libraryDependencies += "com.lihaoyi" %% "requests" % "0.7.0"
libraryDependencies += "com.lihaoyi" %% "ujson" % "2.0.0"
libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.8.1"
libraryDependencies += "com.lambdaworks" %% "jacks" % "2.3.3"