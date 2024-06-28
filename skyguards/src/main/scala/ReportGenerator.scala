import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.json4s._
import org.json4s.jackson.Serialization.write

import scala.util.Random

final case class Location(lat: Float, lon: Float)
final case class Report(
                         id: Int,
                         timestamp: String,
                         pos: Location,
                         nbPeople: Int,
                         surface: Int,
                         speed: Int,
                         battery: Int,
                         temperature: Int
                       )

object ReportGenerator {
  def generateLocation(): Location = {
    val lon = Random.between(2.28, 2.39).toFloat
    val lat = Random.between(48.82, 48.89).toFloat
    Location(lat, lon)
  }

  def generateTimestamp(): String = {
    val now = DateTime.now()
    val formatter = DateTimeFormat.forPattern("dd/MM/yy HH:mm:ss")
    now.toString(formatter)
  }

  def generateRandomInt(min: Int, max: Int): Int = {
    Random.between(min, max)
  }

  def generatePeople(scenario: Int): Int = {
    val rd = Random.between(1, 3)
    if (rd == scenario){
      Random.between(100 * scenario, 800)
    }
    Random.between(1, 500)
  }

  def generateReport(id: Int, scenario: Int): String = {
    val timestamp = generateTimestamp()
    val pos = generateLocation()
    val nbPeople = generatePeople(scenario)
    val surface = generateRandomInt(100, 600)
    val speed = generateRandomInt(25, 35)
    val battery = generateRandomInt(10, 99)
    val temperature = generateRandomInt(20, 80)

    val report = Report(id, timestamp, pos, nbPeople, surface, speed, battery, temperature)
    implicit val formats = DefaultFormats
    write(report)
  }
}

