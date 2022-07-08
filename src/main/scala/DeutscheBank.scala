import scala.language.postfixOps
import spray.json._


object DeutscheBankDomain {
  case class Transaction(id: String, amount: Int, originIban: String, counterPartyIban: String,
                         counterPartyName: String, currencyCode: String, paymentReference: String)
  case class TransactionsRequest(iban: String)
  final case class TransactionsResult(totalItems: Int, limit: Int, offset: Int, transactions: List[Transaction])
  final case class TransactionsRejected(code: Int, message: String, messageId: String)
}

trait DeutscheBankJsonProtocol extends DefaultJsonProtocol {

  import DeutscheBankDomain._

  implicit val transactionFormat: RootJsonFormat[Transaction] = jsonFormat7(Transaction)
  implicit val transactionResultFormat: RootJsonFormat[TransactionsResult] = jsonFormat4(TransactionsResult)
  implicit val transactionRejectedFormat: RootJsonFormat[TransactionsRejected] = jsonFormat3(TransactionsRejected)
}

