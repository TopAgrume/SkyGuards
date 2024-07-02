import kafka.{ConsumerTest, ProducerTest}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer


import java.util.Properties

object Main extends App {

  val scenario = scala.util.Properties.envOrElse("SCENARIO", "1").toInt
  val nbdrone = scala.util.Properties.envOrElse("NB_DRONE", "5").toInt

  val props: Properties = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29093")
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

  List.range(0, nbdrone)
    .map(id => new Thread(new SkyGuard(id, scenario, props)))
    .foreach(t => t.start())

  Thread.sleep(1000 * 60)
}
