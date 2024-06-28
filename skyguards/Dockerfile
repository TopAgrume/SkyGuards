FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.9.0_2.13.10

COPY . /app

WORKDIR /app

RUN sbt compile

RUN sbt run