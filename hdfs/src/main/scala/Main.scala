import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object Main {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder
      .appName("KafkaToHDFS")
      .master("local")
      .getOrCreate()

    import spark.implicits._

    val kafkaDF = spark.read
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:29092")
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

    messagesDF.printSchema()
    println(messagesDF.show())
    println(s"Number of records: ${messagesDF.count()}")

    val hdfsPath = s"hdfs://localhost:9000/user/hdfs/reports/"

    messagesDF.write
      .mode("append")
      .json(hdfsPath)

    spark.stop()
  }
}