package kafka

import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer

import java.time.Duration
import scala.jdk.CollectionConverters._
import java.util.Properties

class ConsumerTest {
  val props: Properties = new Properties()
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092")
  props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  props.put(ConsumerConfig.GROUP_ID_CONFIG, "cg")
  props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") // To read from the beginning
  props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

  val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](props)

  def subscribe(topics: List[String]): Unit = {
    consumer.subscribe(topics.asJava)
  }

  def pullMessages(): Unit = {
    val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofMillis(200))
    consumer.commitSync()
    for (record <- records.asScala) {
      println(s"offset = ${record.offset()}, key = ${record.key()}, value = ${record.value()}")
    }
  }

  def closeConsumer(): Unit = {
    consumer.close()
  }
}