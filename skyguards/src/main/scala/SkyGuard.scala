import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.Properties
import java.util.UUID.randomUUID
import ReportGenerator.generateReport

import org.json4s._
import org.json4s.jackson.Serialization.write

import scala.util.Random

class SkyGuard(id: Int, scenario: Int, properties: Properties) extends Runnable{
  def send(producer : KafkaProducer[String, String], lon : Float, lat : Float) : Unit = {
    Thread.sleep(2000)
    val report = generateReport(id, scenario, lon, lat)
    val new_lon = report.pos.lon
    val new_lat = report.pos.lat

    implicit val formats: Formats = DefaultFormats

    val record = new ProducerRecord[String, String]("reports", randomUUID().toString, write(report))
    producer.send(record)
    producer.flush()
    send(producer, new_lon, new_lat)
    println("Drone -> Sending message")
  }

  def run() : Unit = {
    val producer: KafkaProducer[String, String] = new KafkaProducer(properties)
    val init_lon = Random.between(2.28, 2.39).toFloat
    val init_lat = Random.between(48.82, 48.89).toFloat
    send(producer, init_lon, init_lat)
  }
}
