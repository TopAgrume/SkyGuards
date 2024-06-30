package kafka

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties

class ProducerTest {
  val props: Properties = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092")
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

  val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)

  def pushMessages(key: String, value: String): RecordMetadata = {
    val record = new ProducerRecord[String, String]("reports", key, value)
    producer.send(record).get()
  }

  def closeProducer(): Unit = {
    producer.close()
  }
}