ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.10"

lazy val root = (project in file("."))
  .settings(
    name := "influx",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.0.2",
      "org.apache.spark" %% "spark-sql" % "3.0.2",
      "org.apache.hadoop" % "hadoop-client" % "2.2.0",
      "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.0.2",
      "com.influxdb" % "influxdb-client-scala_2.12" % "6.7.0",
      "com.typesafe.akka" %% "akka-stream" % "2.8.6",
      "com.typesafe.akka" %% "akka-actor-typed" % "2.8.6",
      "com.typesafe.akka" %% "akka-slf4j" % "2.8.6",
      "ch.qos.logback" % "logback-classic" % "1.2.10",
    ),
    dependencyOverrides ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0"
    )
  )