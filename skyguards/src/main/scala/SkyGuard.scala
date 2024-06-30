import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.Properties
import java.util.UUID.randomUUID
import ReportGenerator.generateReport

class SkyGuard(id: Int, scenario: Int, properties: Properties) extends Runnable{

  def send(producer : KafkaProducer[String, String]) : Unit = {
    Thread.sleep(2000)
    val record = new ProducerRecord[String, String]("reports", randomUUID().toString, generateReport(id, scenario))
    producer.send(record)
    producer.flush()
    send(producer)
    println("Drone -> Sending message")
  }

  def run() : Unit = {
    val producer: KafkaProducer[String, String] = new KafkaProducer(properties)
    send(producer)
  }
}
