import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.Encoders

final case class Location(lon: Double, lat: Double)
final case class Report(
                         id: Int,
                         timestamp: String,
                         pos: Location,
                         nbPeople: Int,
                         surface: Int,
                         speed: Int,
                         battery: Int,
                         temperature: Int
                       )

object Main {
  val hdfs_master = "hdfs://localhost:9000/"
  val spark = SparkSession.builder
    .appName("Analysis")
    .master("local")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  import spark.implicits._

  def main(args: Array[String]): Unit = {
    val schema = Encoders.product[Report].schema

    val df = spark.read.schema(schema).json(hdfs_master + "user/hdfs/reports/*.json")
    df.show()

    df.select("id").distinct().orderBy("id").collect().foreach { row =>
      val droneId = row.getAs[Int]("id")
      println(s"Statistics for drone $droneId:")
      calculateStats(df, droneId)
    }

    spark.stop()
  }

  def calculateStats(data: DataFrame, droneId: Int): Unit = {
    val filteredData = data.filter($"id" === droneId)

    val peoplePerM2 = filteredData.select(avg($"nbPeople" / $"surface")).as[Double].first()
    val maxTemperature = filteredData.agg(max("temperature")).as[Int].first()
    val avgTemperature = filteredData.agg(avg("temperature")).as[Double].first()
    val lastBattery = filteredData.orderBy(desc("timestamp")).select("battery").as[Int].first()
    val avgSpeed = filteredData.agg(avg("speed")).as[Double].first()
    val firstPos = filteredData.orderBy(asc("timestamp")).select("pos.*").as[Location].first()
    val lastPos = filteredData.orderBy(desc("timestamp")).select("pos.*").as[Location].first()

    // Display statistics
    println(f"Number of people per m2: $peoplePerM2%.2f")
    println(s"Maximum temperature: $maxTemperature")
    println(s"Average temperature: $avgTemperature")
    println(s"Last battery level: $lastBattery")
    println(s"Average speed: $avgSpeed")
    println(s"First position: ${firstPos.lon}, ${firstPos.lat}")
    println(s"Last position: ${lastPos.lon}, ${lastPos.lat}")
    println()
  }
}