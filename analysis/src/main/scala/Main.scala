import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.sql.functions._

object Main {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("Analysis")
      .master("local")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")
    import spark.implicits._

    val hdfs_master = "hdfs://localhost:9000/"

    val df_csv = spark.read.option("inferSchema", "true").option("header", "true").csv(hdfs_master + "data/openbeer/breweries/breweries.csv")
    df_csv.show()

    spark.stop()
  }
}
