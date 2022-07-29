package pipeline

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{ TestSink, TestSource }
import akka.http.scaladsl.model.StatusCodes.OK
import akka.testkit.TestKit
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.io.Source
import scala.util.Success

class FlowsSpec
    extends TestKit(ActorSystem("FlowsSpecSystem"))
    with AnyWordSpecLike
    with BeforeAndAfterAll
    with MockitoSugar
    with FailFastCirceSupport {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "A decodingFlow" should {

    import Events.BankingDomain._

    "decode a ConsumerRecord to event GetTransactionsEvent" in {
      import Flows.decodingFlow

      val testSource              = TestSource.probe[ConsumerRecord[String, String]]
      val testSink                = TestSink.probe[GetTransactionsEvent]
      val record: String          = Source.fromResource("pipeline/getTransactionsRecord.json").mkString
      val materialized            = testSource.via(decodingFlow).toMat(testSink)(Keep.both).run()
      val (publisher, subscriber) = materialized

      publisher
        .sendNext(new ConsumerRecord("topic", 0, 0L, "key", record))
        .sendComplete()

      subscriber
        .request(1)
        .expectNext(GetTransactionsEvent("token", "4321-4321-4321-4321"))
        .expectComplete()
    }
  }

  "A requestFlow" should {
    import io.circe.parser.decode
    import accounts.db.DeutscheBankAccount
    import accounts.db.DeutscheBankAccount.Transaction
    import accounts.db.DeutscheBankAccount.Response._
    import accounts.db.DeutscheBankAccount.Codecs._
    import clients._
    import Events.BankingDomain._
    import Flows.requestFlow
    import system.dispatcher

    "request bank transaction with the GetTransactionsEvent event" in {
      // mock DeutscheBankAccount
      val getTransactionsResponseText: String = Source.fromResource("db/getTransactionsResponse.json").mkString
      val getTransactions                     = decode[GetTransactionsResponse](getTransactionsResponseText).toOption.get
      val successResponseBody                 = SuccessResponseBody(getTransactions)
      val mockResponse                        = Response[GetTransactionsResponse](OK.intValue, successResponseBody, Map())
      val deutscheBankAccount                 = mock[DeutscheBankAccount]
      when(deutscheBankAccount.getTransactions("4321-4321-4321-4321")).thenReturn(Future.successful(mockResponse))

      // test flows with a test source and a test sink
      val testSource              = TestSource.probe[GetTransactionsEvent]
      val testSink                = TestSink.probe[Future[List[Transaction]]]
      val materialized            = testSource.via(requestFlow).toMat(testSink)(Keep.both).run()
      val (publisher, subscriber) = materialized

      publisher
        .sendNext(GetTransactionsEvent("token", "4321-4321-4321-4321"))
        .sendComplete()

      subscriber
        .request(1)
        .expectNext(Future.successful(getTransactions.transactions)) // TODO: future check
        .expectComplete()
    }
  }
}
