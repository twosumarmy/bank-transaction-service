package accounts.db

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import clients.{ AkkaHttpClient, Response }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.{ Decoder, HCursor }
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ ExecutionContext, Future }

object DeutscheBankAccount {

  object Domain {
    val Simulation = "https://simulator-api.db.com"
  }

  object Api {
    val bankingCashAccounts = "/gw/dbapi/banking/cashAccounts/v2"
    val bankingTransactions = "/gw/dbapi/banking/transactions/v2"
  }

  // commands = messages
  sealed trait Command
  object Command {
    case class GetBankAccounts(iban: String) extends Command
    case class GetTransactions(iban: String) extends Command
  }

  // events = to persist (e.g kafka)
  trait Event

  // state

  /**
   * A cash account
   * @param iban
   *   The IBAN of this account.
   * @param currencyCode
   *   ISO 4217 Alpha 3 currency code.
   * @param bic
   *   BIC of the cash account.
   * @param accountType
   *   Type of the cash account.
   * @param currentBalance
   *   Booked balance in EUR.
   * @param productDescription
   *   Description of the product.
   */
  case class BankAccount(
    iban: String,
    currencyCode: String,
    bic: String,
    accountType: String,
    currentBalance: Double,
    productDescription: String
  )

  /**
   * A cash account transaction.
   * @param id
   *   The id of the requested transaction. This id is NOT immutable and is to be used primarily in conjunction with the
   *   transaction notification API when requesting a specific transaction.
   * @param amount
   *   Amount of the transaction. If the amount is positive, the customer received money, if the amount is negative the
   *   customer lost money.
   * @param originIban
   *   The IBAN of this account.
   * @param counterPartyIban
   *   The IBAN of the counter party.
   * @param counterPartyName
   *   Name of the counter party.
   * @param currencyCode
   *   ISO 4217 Alpha 3 currency code.
   * @param paymentReference
   *   Payment reference.
   * @param bookingDate
   *   Booking date. In the format YYYY-MM-DD.
   * @param transactionCode
   *   Specifies the business transaction code (GVC).
   * @param externalBankTransactionDomainCode
   *   Specifies the bank transaction code domain, as published in an external bank transaction code domain code list
   *   according to ISO 20022.
   * @param externalBankTransactionFamilyCode
   *   Specifies the external family code of the bank transaction code in the format of character string with a maximum
   *   length of 4 characters according to ISO 20022.
   * @param externalBankTransactionSubFamilyCode
   *   Specifies the bank transaction code sub-family, as published in an external bank transaction code sub-family code
   *   list according to ISO 20022.
   * @param mandateReference
   *   MandateReference of the direct debit transaction, max length 35 characters.
   * @param creditorId
   *   Creditor ID, max length 35 characters.
   * @param e2eReference
   *   Unique identification given by the orderer to identify the order.
   * @param paymentIdentification
   *   Unique and unambiguous identification of a transaction, as assigned by any of the customer parties on the
   *   initiating side.
   * @param valueDate
   *   Value date of Transaction in ISO 8601 format (YYYY-MM-DD). In very specific cases, technical date is possible for
   *   e.g., (2020-02-30).
   */
  case class Transaction(
    id: String,
    amount: Double,
    originIban: String,
    counterPartyIban: Option[String],
    counterPartyName: String,
    currencyCode: String,
    paymentReference: String,
    bookingDate: LocalDate,
    transactionCode: String,
    externalBankTransactionDomainCode: String,
    externalBankTransactionFamilyCode: String,
    externalBankTransactionSubFamilyCode: String,
    mandateReference: String,
    creditorId: String,
    e2eReference: String,
    paymentIdentification: String,
    valueDate: LocalDate
  )

  // responses
  sealed trait Response
  object Response {
    case class GetBankAccountsResponse(totalItems: Int, limit: Int, offset: Int, accounts: List[BankAccount])
        extends Response
    case class GetTransactionsResponse(totalItems: Int, limit: Int, offset: Int, transactions: List[Transaction])
        extends Response
  }

  object Codecs {
    import DeutscheBankAccount.Response.{ GetBankAccountsResponse, GetTransactionsResponse }

    implicit val dateISO8601Decoder: Decoder[LocalDate] =
      Decoder.decodeLocalDateWithFormatter(DateTimeFormatter.ISO_LOCAL_DATE)

    implicit val transactionDecoder: Decoder[Transaction] = (c: HCursor) =>
      for {
        id                                   <- c.downField("id").as[String]
        amount                               <- c.downField("amount").as[Double]
        originIban                           <- c.downField("originIban").as[String]
        counterPartyIban                     <- c.downField("counterPartyIban").as[Option[String]]
        counterPartyName                     <- c.downField("counterPartyName").as[String]
        currencyCode                         <- c.downField("currencyCode").as[String]
        paymentReference                     <- c.downField("paymentReference").as[String]
        bookingDate                          <- c.downField("bookingDate").as[LocalDate]
        transactionCode                      <- c.downField("transactionCode").as[String]
        externalBankTransactionDomainCode    <- c.downField("externalBankTransactionDomainCode").as[String]
        externalBankTransactionFamilyCode    <- c.downField("externalBankTransactionFamilyCode").as[String]
        externalBankTransactionSubFamilyCode <- c.downField("externalBankTransactionSubFamilyCode").as[String]
        mandateReference                     <- c.downField("mandateReference").as[String]
        creditorId                           <- c.downField("creditorId").as[String]
        e2eReference                         <- c.downField("e2eReference").as[String]
        paymentIdentification                <- c.downField("paymentIdentification").as[String]
        valueDate                            <- c.downField("valueDate").as[LocalDate]
      } yield Transaction(
        id,
        amount,
        originIban,
        counterPartyIban,
        counterPartyName,
        currencyCode,
        paymentReference,
        bookingDate,
        transactionCode,
        externalBankTransactionDomainCode,
        externalBankTransactionFamilyCode,
        externalBankTransactionSubFamilyCode,
        mandateReference,
        creditorId,
        e2eReference,
        paymentIdentification,
        valueDate
      )

    implicit val bankAccountDecoder: Decoder[BankAccount] = (c: HCursor) =>
      for {
        iban               <- c.downField("iban").as[String]
        currencyCode       <- c.downField("currencyCode").as[String]
        bic                <- c.downField("bic").as[String]
        accountType        <- c.downField("accountType").as[String]
        currentBalance     <- c.downField("currentBalance").as[Double]
        productDescription <- c.downField("productDescription").as[String]
      } yield BankAccount(iban, currencyCode, bic, accountType, currentBalance, productDescription)

    implicit val getTransactionsResponseDecoder: Decoder[GetTransactionsResponse] = (c: HCursor) =>
      for {
        totalItems   <- c.downField("totalItems").as[Int]
        limit        <- c.downField("limit").as[Int]
        offset       <- c.downField("offset").as[Int]
        transactions <- c.downField("transactions").as[List[Transaction]]
      } yield GetTransactionsResponse(totalItems, limit, offset, transactions)

    implicit val getBankAccountsResponseDecoder: Decoder[GetBankAccountsResponse] = (c: HCursor) =>
      for {
        totalItems <- c.downField("totalItems").as[Int]
        limit      <- c.downField("limit").as[Int]
        offset     <- c.downField("offset").as[Int]
        accounts   <- c.downField("accounts").as[List[BankAccount]]
      } yield GetBankAccountsResponse(totalItems, limit, offset, accounts)
  }
}

/**
 * Deutsche Bank API Client based on ReST operations. More information at
 * [[https://developer.db.com/apiexplorer/terminal/v2/banking/cashAccounts/db Deutsche Bank API Program]].
 * @param accessToken
 *   Specifies the access token for authorization.
 */
class DeutscheBankAccount(var accessToken: String, val baseUrl: String = DeutscheBankAccount.Domain.Simulation)(implicit
  actorSystem: ActorSystem
) extends AkkaHttpClient
    with FailFastCirceSupport {

  import DeutscheBankAccount.Api._
  import DeutscheBankAccount.Domain._
  import DeutscheBankAccount.Response._
  import DeutscheBankAccount.Codecs._

  def setAccessToken(token: String): Unit =
    accessToken = token

  /**
   * Reads all accounts of the current user. If given IBAN is not valid or does not represent an account of the current
   * user, an empty result is returned.
   * @param iban
   *   representing an account of the current user
   * @return
   *   [[Future[Response[GetBankAccountsResponse]]]
   */
  def getBankAccounts(iban: String = "")(implicit ec: ExecutionContext): Future[Response[GetBankAccountsResponse]] =
    get[GetBankAccountsResponse](
      getUriWith(bankingCashAccounts),
      params = if (iban == "") Map() else Map("iban" -> iban),
      headers = getHeaders
    )

  /**
   * Reads the transactions for cash accounts for the given customer. The API provides in default up to 13 months of
   * transaction history. If given IBAN is not valid or does not represent an account of the current customer, an empty
   * result is returned.
   * @param iban
   *   representing an account of the current user
   * @return
   *   [[Future[Response[GetTransactionsResponse]]]
   */
  def getTransactions(iban: String)(implicit ec: ExecutionContext): Future[Response[GetTransactionsResponse]] =
    get[GetTransactionsResponse](
      getUriWith(bankingTransactions),
      params = Map("iban" -> iban),
      headers = getHeaders
    )

  private def getHeaders: Map[String, String] = Map("Authorization" -> s"Bearer $accessToken")

  private def getUriWith(path: String): String = Uri(baseUrl).withPath(Path(path)).toString()
}
