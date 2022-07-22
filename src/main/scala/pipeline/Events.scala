package pipeline

object Events {
  sealed trait KafkaEvent

  object BankingDomain {
    case class GetTransactionsEvent(accessToken: String, iban: String) extends KafkaEvent
  }
}
