import kafka.{ConsumerTest, ProducerTest}
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties

object Main extends App {
  val consumerTest = new ConsumerTest()
  val producerTest = new ProducerTest()

  consumerTest.subscribe(List("reports"))

  Thread.sleep(4000)
  println("Pushing new message")

  producerTest.pushMessages("127", "zizou")
  producerTest.pushMessages("128", "fifou")
  producerTest.pushMessages("129", "lilou")
  producerTest.pushMessages("139", "mimou")


  for (_ <- 1 to 3) {
    consumerTest.pullMessages()
    Thread.sleep(2000)
    println("pulling messages")
  }

  consumerTest.closeConsumer()
  producerTest.closeProducer()
}
