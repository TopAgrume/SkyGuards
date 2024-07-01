ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "SkyGuards",
    version := "0.1",
    libraryDependencies += "org.apache.kafka" %% "kafka" % "2.6.0",
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.7.0-M11",
    libraryDependencies += "joda-time" % "joda-time" % "2.10.13"
  )
