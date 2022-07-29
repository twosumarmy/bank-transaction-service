package pipeline

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

object ProducerApp extends App {
  implicit val system: ActorSystem          = ActorSystem("producer-sys")
  implicit val mat: Materializer            = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val config           = ConfigFactory.load()
  val producerConfig   = config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

  val produce: Future[Done] =
    Source(1 to 100)
      .map(value => new ProducerRecord[String, String]("banking", s"""{\"accessToken\": \"${value}\"}"""))
      .runWith(Producer.plainSink(producerSettings))

  produce onComplete {
    case Success(_)   => println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }
}
