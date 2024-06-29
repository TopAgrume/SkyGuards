import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object SparkStreaming {
  def main(args: Array[String]): Unit = {
    println("Init Spark Session")
    val spark =  SparkSession.builder()
      .appName("SkyGuardsSparkProcess")
      .master("local[*]")
      .getOrCreate()
    spark.sparkContext.setLogLevel("OFF")

    println("Reading from kafka-stream-1")
    val content_df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")
      .option("subscribe", "reports")
      .option("startingOffsets", "earliest")
      .load()

    val transformed_df = content_df
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .withColumn("value", upper(col("value")))

    println("Writing to Kafka topic 'alerts'")
    val query = transformed_df
      .selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")
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
    println("ended")
}
}