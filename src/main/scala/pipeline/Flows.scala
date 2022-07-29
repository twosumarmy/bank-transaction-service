package pipeline

import akka.actor.ActorSystem
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.Flow
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.parser.decode
import io.circe.generic.auto._
import org.apache.kafka.clients.consumer.ConsumerRecord
import Events.BankingDomain._
import accounts.db.DeutscheBankAccount
import accounts.db.DeutscheBankAccount.Response.GetTransactionsResponse
import accounts.db.DeutscheBankAccount.Transaction
import akka.NotUsed
import clients.{ FailureResponseBody, Response, SuccessResponseBody }
import scala.concurrent.Future

/**
 * A collection of operators which has exactly one input and output, which connects its upstream and downstream by
 * transforming the data elements flowing through it.
 */
object Flows extends FailFastCirceSupport {

  implicit val system: ActorSystem        = ActorSystem("consumerActor")
  implicit val materializer: Materializer = ActorMaterializer()
  import system.dispatcher

  object Exceptions {
    abstract class FlowException(message: String)    extends Error(message)
    case class RequestFlowException(message: String) extends FlowException(message)
  }

  import Exceptions._

  val decodingFlow: Flow[ConsumerRecord[String, String], GetTransactionsEvent, _] =
    Flow[ConsumerRecord[String, String]].map { consumerRecord =>
      decode[GetTransactionsEvent](consumerRecord.value()) match {
        case Left(error)  => throw error
        case Right(event) => event
      }
    }

  val requestFlow: Flow[GetTransactionsEvent, Future[List[Transaction]], NotUsed] =
    Flow[GetTransactionsEvent].map { event =>
      val account                    = new DeutscheBankAccount(event.accessToken)
      val transactionsResponseFuture = account.getTransactions(event.iban)
      transactionsResponseFuture.map {
        case Response(_, body: SuccessResponseBody[GetTransactionsResponse], _) => body.bodyObject.transactions
        case Response(_, body: FailureResponseBody[GetTransactionsResponse], _) =>
          throw RequestFlowException(body.error)
      }
    }
}
