import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object Main {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder
      .appName("KafkaToHDFS")
      .master("local")
      .getOrCreate()

    val kafkaDF = spark.read
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")
      .option("subscribe", "reports")
      .option("startingOffsets", "earliest")
      .load()

    val messagesDF = kafkaDF.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")

    messagesDF.printSchema()
    println(messagesDF.show())
    println(s"Number of records: ${messagesDF.count()}")

    val hdfsPath = s"hdfs://localhost:9000/user/hdfs/reports/"

    messagesDF.write
      .mode("append")
      .json(hdfsPath)
  }
}