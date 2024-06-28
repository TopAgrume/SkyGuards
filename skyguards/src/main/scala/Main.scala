import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties

object Main {

  def main(args: Array[String]) : Unit = {

    val scenario = scala.util.Properties.envOrElse("SCENARIO", "1").toInt
    val nbdrone = scala.util.Properties.envOrElse("NB_DRONE", "5").toInt
    val host = scala.util.Properties.envOrElse("KAFKA_HOST", "localhost:9092")

    val prop: Properties = new Properties()
    prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, host)
    prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

    List.range(0, nbdrone)
        .map(id => new Thread(new SkyGuard(id, scenario, prop)))
        .foreach(t => t.start())

    Thread.sleep(1000 * 5)
  }
}
