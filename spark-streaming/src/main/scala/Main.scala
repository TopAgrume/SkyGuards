import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object Main {
  def main(args: Array[String]): Unit = {
    println("Init Spark Session")
    val spark =  SparkSession.builder()
      .appName("SkyGuardsSparkProcess")
      .master("local")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    // Define the schema for the JSON data
    val schema = new StructType()
      .add("id", IntegerType)
      .add("timestamp", StringType)
      .add("pos", new StructType()
        .add("lat", DoubleType)
        .add("lon", DoubleType))
      .add("nbPeople", IntegerType)
      .add("surface", IntegerType)
      .add("speed", IntegerType)
      .add("battery", IntegerType)
      .add("temperature", IntegerType)

    println("Reading from kafka-stream-1")
    val content_df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka-broker-1:9092") // kafka-broker-1:9092
      .option("subscribe", "reports")
      .option("startingOffsets", "earliest")
      .load()

    // Select and parse the value column
    val parsed_df = content_df
      .selectExpr("CAST(value AS STRING)")
      .select(from_json(col("value"), schema).as("data"))
      .select("data.*")

    // Compute the density
    val density_df = parsed_df.withColumn("density", col("nbPeople") / col("surface"))

    // Filter rows where density exceeds the threshold
    val threshold = 7
    val alert_df = density_df.filter(col("density") > threshold)

    // Convert the alert messages back to JSON
    val alert_json_df = alert_df.selectExpr("CAST(id AS STRING) AS key", "to_json(struct(*)) AS value")

    println("Writing to Kafka topic 'alerts'")
    val query = alert_json_df
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka-broker-1:9092") // kafka-broker-1:9092
      .option("topic", "alerts")
      .option("checkpointLocation", "/tmp/kafka-checkpoint")
      .start()

    /*println("Logging in the console:")
    val query = content_df
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .writeStream
      .format("console")
      .outputMode("append")
      .start() */

    query.awaitTermination()
    println("Spark process ended")
  }
}