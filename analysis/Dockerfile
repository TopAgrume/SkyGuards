FROM sbtscala/scala-sbt:11.0.15_1.6.2_2.13.8

COPY . /app

WORKDIR /app

RUN sbt clean

RUN sbt compile

ENTRYPOINT sbt run