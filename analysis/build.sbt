ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.10"

lazy val root = (project in file("."))
  .settings(
    name := "analysis",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.0.2",
      "org.apache.spark" %% "spark-sql" % "3.0.2",
      "org.apache.hadoop" % "hadoop-client" % "2.2.0"
  )
)
