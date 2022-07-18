package accounts.db

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.testkit.TestKit
import clients.{ FailureResponseBody, SuccessResponseBody }
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, equalTo, get, urlEqualTo }
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.{ AnyWordSpec, AsyncWordSpecLike }
import tags.ApiCall
import java.time.LocalDate
import scala.io.Source

class DeutscheBankAccountSpec
    extends TestKit(ActorSystem("DeutscheBankAccountSpec"))
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with FailFastCirceSupport {

  import io.circe.parser._
  import DeutscheBankAccount.Api._
  import DeutscheBankAccount.Codecs._
  import DeutscheBankAccount.Response._

  val wireMockServer  = new WireMockServer(wireMockConfig().dynamicPort())
  val applicationJson = "application/json"

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
    TestKit.shutdownActorSystem(system)
  }

  "getTransactions" should {
    val getTransactionsResponseText: String = Source.fromResource("db/getTransactionsResponse.json").mkString
    val expectedGetTransactionsResponse     = decode[GetTransactionsResponse](getTransactionsResponseText).toOption.get

    "return a SuccessResponseBody with a body of type GetTransactionsResponse" taggedAs ApiCall in {
      val client   = new DeutscheBankAccount("token", baseUrl = wireMockServer.baseUrl())
      val response = aResponse()
        .withStatus(OK.intValue)
        .withHeader("Content-Type", applicationJson)
        .withBody(getTransactionsResponseText)
      wireMockServer.stubFor(
        get(urlEqualTo(s"${bankingTransactions}?iban=123"))
          .withHeader("Authorization", equalTo("Bearer token"))
          .willReturn(response)
      )

      client.getTransactions("123").map { response =>
        assert(response.status == 200)
        response.body match {
          case SuccessResponseBody(bodyObject) => assert(bodyObject == expectedGetTransactionsResponse)
          case FailureResponseBody(error)      => fail(error)
        }
      }
    }
  }

  "getBankAccounts" should {
    val getBankAccountsResponseText: String = Source.fromResource("db/getBankAccountsResponse.json").mkString
    val expectedGetBankAccountsResponse     = decode[GetBankAccountsResponse](getBankAccountsResponseText).toOption.get

    "return a SuccessResponseBody with a body of type GetBankAccountsResponse" taggedAs ApiCall in {
      val client   = new DeutscheBankAccount("token", baseUrl = wireMockServer.baseUrl())
      val response = aResponse()
        .withStatus(OK.intValue)
        .withHeader("Content-Type", applicationJson)
        .withBody(getBankAccountsResponseText)
      wireMockServer.stubFor(
        get(urlEqualTo(s"${bankingCashAccounts}?iban=123"))
          .withHeader("Authorization", equalTo("Bearer token"))
          .willReturn(response)
      )

      client.getBankAccounts(iban = "123").map { response =>
        assert(response.status == 200)
        response.body match {
          case SuccessResponseBody(bodyObject) => assert(bodyObject == expectedGetBankAccountsResponse)
          case FailureResponseBody(error)      => fail(error)
        }
      }
    }
  }
}

class DeutscheBankAccountCodecsSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with FailFastCirceSupport {

  import io.circe.parser._
  import DeutscheBankAccount.{ BankAccount, Transaction }
  import DeutscheBankAccount.Codecs._
  import DeutscheBankAccount.Response._

  "transactionDecoder" should {
    val expectedTransaction = Transaction(
      "2rVXBkxUMbLWfsKBr-tdy9jPe7BDme3ISVkHwTBTFFeYoJyPxRo8lIIKr5rYei5xJVrm1HffjxU99ksk9jHwsnfo8fSBJvSg0J8GaoGGhpKYLBTt43kPjJ-aLyztHuWa",
      -200.00,
      "DE00500700100200000027",
      None,
      "Deutsche Bank",
      "EUR",
      "Barauszahlung, Schule",
      LocalDate.parse("2021-11-24"),
      "123",
      "D001",
      "CCRD",
      "CWDL",
      "ABMX0355443",
      "DE0111100004544221",
      "E2E - Reference",
      "ZKLE 911/XC:121600 ABC",
      LocalDate.parse("2018-04-23")
    )

    "decode to object Transaction" in {
      val text: String = Source.fromResource("db/transaction.json").mkString
      decode[Transaction](text) match {
        case Right(transaction) => assert(transaction == expectedTransaction)
        case Left(error)        => fail(error.toString)
      }
    }
  }

  "bankAccountDecoder" should {
    val expectedBankAccount = BankAccount(
      "DE00500700100200000027",
      "EUR",
      "DEUTDEFFXXX",
      "CURRENT_ACCOUNT",
      100.95,
      "Persönliches Konto"
    )

    "decode to object BankAccount" in {
      val text: String = Source.fromResource("db/bankAccount.json").mkString
      decode[BankAccount](text) match {
        case Right(bankAccount) => assert(bankAccount == expectedBankAccount)
        case Left(error)        => fail(error.toString)
      }
    }
  }

  "getTransactionsResponseDecoder" should {
    "decode to object GetTransactionsResponse" in {
      val text: String = Source.fromResource("db/getTransactionsResponse.json").mkString
      decode[GetTransactionsResponse](text) match {
        case Right(getTransactionsResponse) => assert(getTransactionsResponse.transactions.length == 10)
        case Left(error)                    => fail(error.toString)
      }
    }
  }

  "getBankAccountsResponseDecoder" should {
    "decode to object GetBankAccountsResponse" in {
      val text: String = Source.fromResource("db/getBankAccountsResponse.json").mkString
      decode[GetBankAccountsResponse](text) match {
        case Right(getBankAccountsResponse) => assert(getBankAccountsResponse.accounts.length == 1)
        case Left(error)                    => fail(error.toString)
      }
    }
  }
}
