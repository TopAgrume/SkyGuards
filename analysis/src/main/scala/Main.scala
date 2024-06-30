import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._

object Main {
  val hdfs_master = "hdfs://localhost:9000/"
  val spark = SparkSession.builder
    .appName("Analysis")
    .master("local")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  import spark.implicits._

  def main(args: Array[String]): Unit = {


    val df = spark.read.option("inferSchema", "true").option("multiline", "true").format("json").load(hdfs_master + "data/openbeer/breweries/test.json")
    df.show()

    avgStatsOfDrone(df, 1)
    spark.stop()
  }

  def avgStatsOfDrone(data: DataFrame, droneId: Int): Unit = {
    val filteredData = data.filter($"id" === droneId)
    val avgTemperature = filteredData.agg(avg("temperature")).as[Double].first()
    val avgBattery = filteredData.agg(avg("battery")).as[Double].first()

    println(s"The average temperature of drone $droneId is: $avgTemperature")
    println(s"The average battery level of drone $droneId is: $avgBattery")
  }
}
