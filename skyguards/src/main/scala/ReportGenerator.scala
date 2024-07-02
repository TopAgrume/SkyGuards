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
    def generateLocation(lon: Float, lat: Float): Location = {
      // Between 0    and 0.33 move lon & lat
      // Between 0.33 and 0.66 move only lat
      // Between 0.66 and 1    move only lon
      val moveType = Random.between(0.0, 1.0)

      val moveLon = Random.between(0.0, 0.11).toFloat
      val moveLat = Random.between(0.0, 0.07).toFloat

      val lonModifier = Random.between(0, 2)
      val latModifier = Random.between(0, 2)

      // To prevent very significant movements
      val factor = 0.1

      val newLon = moveType match {
        case t if t < 0.33 => lon + (if (lonModifier == 0) -moveLon else moveLon) * factor
        case t if t < 0.66 => lon
        case _ => lon + (if (lonModifier == 0) -moveLon else moveLon) * factor
      }

      val newLat = moveType match {
        case t if t >= 0.33 && t < 0.66 => lat + (if (latModifier == 0) -moveLat else moveLat) * factor
        case _ => lat
      }

      // To make sure it does not goes beyond paris limit
      val boundedLon = newLon.max(2.28f).min(2.39f).toFloat
      val boundedLat = newLat.max(48.82f).min(48.89f).toFloat

      Location(boundedLat,boundedLon)
    }

    def generateTimestamp(): String = {
      val now = DateTime.now()
      val formatter = DateTimeFormat.forPattern("dd/MM/yy HH:mm:ss")
      now.toString(formatter)
    }

    def generateRandomInt(min: Int, max: Int): Int = {
      Random.between(min, max)
    }

    def generatePeople(scenario: Int, surface: Int): Int = {

      val max_people : Int = surface * 10

      scenario match {
        case 1 =>
          if (Random.nextDouble() < 0.9) {
            Random.between(surface, (surface * 6).toInt)
          } else {
            Random.between((surface * 7).toInt, max_people)
          }
        case 2 =>
          if (Random.nextDouble() < 0.8) {
            Random.between(surface, (surface * 6).toInt)
          } else {
            Random.between((surface * 7).toInt, max_people)
          }
        case 3 =>
          if (Random.nextDouble() < 0.7) {
            Random.between(surface, (surface * 6).toInt)
          } else {
            Random.between((surface * 7).toInt, max_people)
          }
      }
    }

    def generateReport(id: Int, scenario: Int, lon : Float, lat : Float): Report = {
      val timestamp = generateTimestamp()
      val pos = generateLocation(lon, lat)
      val surface = generateRandomInt(100, 400)
      val nbPeople = generatePeople(scenario, surface)
      val speed = generateRandomInt(25, 35)
      val battery = generateRandomInt(10, 99)
      val temperature = generateRandomInt(20, 80)

      Report(id, timestamp, pos, nbPeople, surface, speed, battery, temperature)
    }
  }

