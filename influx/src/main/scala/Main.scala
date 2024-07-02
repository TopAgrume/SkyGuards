import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.{Keep, Source}
import com.influxdb.annotations.{Column, Measurement}
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.scala.InfluxDBClientScalaFactory
import com.influxdb.client.write.Point
import akka.stream.{Materializer, SystemMaterializer}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.time.{Instant, LocalDateTime, ZoneOffset}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row

import java.time.format.DateTimeFormatter

object InfluxDBService {

  // Setting environment variables as application constants
  val DOCKER_INFLUXDB_INIT_ADMIN_TOKEN = sys.env.getOrElse("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", "6cf082b94f7132a1487bc05729e7a3ec08db8b8d811bf8194508ed4b15d7c353")
  val DOCKER_INFLUXDB_INIT_ORG = sys.env.getOrElse("DOCKER_INFLUXDB_INIT_ORG", "skyguards")
  val DOCKER_INFLUXDB_INIT_BUCKET = sys.env.getOrElse("DOCKER_INFLUXDB_INIT_BUCKET", "website")
  val DOCKER_INFLUXDB_INIT_PORT = sys.env.getOrElse("DOCKER_INFLUXDB_INIT_PORT", "8086").toInt
  val DOCKER_INFLUXDB_INIT_HOST = sys.env.getOrElse("DOCKER_INFLUXDB_INIT_HOST", "localhost")

  // Retrieve InfluxDB configuration from the constants
  val influxUrl = s"http://$DOCKER_INFLUXDB_INIT_HOST:$DOCKER_INFLUXDB_INIT_PORT"
  val influxToken = DOCKER_INFLUXDB_INIT_ADMIN_TOKEN
  val influxOrg = DOCKER_INFLUXDB_INIT_ORG
  val influxBucket = DOCKER_INFLUXDB_INIT_BUCKET

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "examples")
  implicit val materializer: Materializer = SystemMaterializer(system).materializer

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder
      .appName("KafkaToInflux")
      .master("local")
      .getOrCreate()

    import spark.implicits._

    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka-broker-1:9092")
      .option("subscribe", "reports")
      .option("startingOffsets", "earliest")
      .load()

    // Define the schema of your JSON data
    val jsonSchema = new StructType()
      .add("id", IntegerType)
      .add("timestamp", StringType)
      .add("pos", new StructType()
        .add("lon", DoubleType)
        .add("lat", DoubleType))
      .add("nbPeople", IntegerType)
      .add("surface", IntegerType)
      .add("speed", IntegerType)
      .add("battery", IntegerType)
      .add("temperature", IntegerType)

    val messagesDF = kafkaDF
      .selectExpr("CAST(value AS STRING) as json")
      .select(from_json(col("json"), jsonSchema).as("data"))
      .select("data.*")
    // Define the date formatter for the custom date format
    val inputDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss")

    val client = InfluxDBClientScalaFactory.create(influxUrl, influxToken.toCharArray, influxOrg, influxBucket)
    val writeApi = client.getWriteScalaApi

    // Process each row and write to InfluxDB
    // Process each row and write to InfluxDB
    messagesDF.writeStream
      .foreachBatch((batchDF: org.apache.spark.sql.Dataset[org.apache.spark.sql.Row], batchId: Long) => {
        batchDF.collect().foreach { row =>
          val id = row.getAs[Int]("id")
          val timestampStr = row.getAs[String]("timestamp")
          val localDateTime = LocalDateTime.parse(timestampStr, inputDateFormatter).minusHours(2)
          // Ensure the timestamp has zero milliseconds
          val formattedTimestamp = localDateTime.withNano(0).toInstant(ZoneOffset.UTC)

          val lon = row.getAs[Row]("pos").getAs[Double]("lon")
          val lat = row.getAs[Row]("pos").getAs[Double]("lat")
          val nbPeople = row.getAs[Int]("nbPeople")
          val surface = row.getAs[Int]("surface")

          // Create a Point object for each record
          val point = Point.measurement("drone")
            .addTag("id", id.toString)
            .addField("lon", lon)
            .addField("lat", lat)
            .addField("nbPeople", nbPeople)
            .addField("density", nbPeople/surface)
            .time(formattedTimestamp.toEpochMilli, WritePrecision.MS)

          val sourcePoint = Source.single(point)
          val sinkPoint = writeApi.writePoint()
          val materializedPoint = sourcePoint.toMat(sinkPoint)(Keep.right)
          Await.result(materializedPoint.run(), Duration.Inf)
        }
      })
      .start()
      .awaitTermination()

    spark.stop()
    client.close()
    system.terminate()
  }

  @Measurement(name = "drone")
  class Drone() {
    @Column(tag = true)
    var id: Int = _
    @Column
    var lon: Double = _
    @Column
    var lat: Double = _
    @Column
    var nbPeople: Int = _
    @Column
    var density: Int = _
    @Column(timestamp = true)
    var timestamp: Instant = _
  }
}
