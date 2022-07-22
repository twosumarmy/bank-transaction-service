package pipeline

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.Flow
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.parser.decode
import io.circe.generic.auto._
import org.apache.kafka.clients.consumer.ConsumerRecord
import Events.BankingDomain._

/**
 * A collection of operators which has exactly one input and output, which connects its upstream and downstream by
 * transforming the data elements flowing through it.
 */
object Flows extends FailFastCirceSupport {

  implicit val system: ActorSystem        = ActorSystem("consumerActor")
  implicit val materializer: Materializer = ActorMaterializer()

  val decodingFlow: Flow[ConsumerRecord[String, String], GetTransactionsEvent, _] =
    Flow[ConsumerRecord[String, String]].map { consumerRecord =>
      decode[GetTransactionsEvent](consumerRecord.value()) match {
        case Left(error)  => throw error
        case Right(event) => event
      }
    }
}
