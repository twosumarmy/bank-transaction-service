import DeutscheBankDomain.TransactionsResult
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.Future
import scala.util.{Failure, Success}


class BaseHTTPClient() {

  implicit val system: ActorSystem = ActorSystem("BaseHTTPClient")

  def get(uri: String): Future[HttpResponse] = Http().singleRequest(
    HttpRequest(uri = uri)
  )
}

class DeutscheBankClient extends BaseHTTPClient with DeutscheBankJsonProtocol with SprayJsonSupport {

  import system.dispatcher

  def getTransactions: Future[TransactionsResult] = {
    get("http://127.0.0.1:8080/gw/dbapi/banking/transaction?iban=123")
      .flatMap { response => Unmarshal(response.entity).to[TransactionsResult] }
  }
}

object SingleRequest extends App {
  implicit val system: ActorSystem = ActorSystem("SingleRequest")
  import system.dispatcher

  val responseFuture: Future[HttpResponse] = Http().singleRequest(
    HttpRequest(uri = "http://127.0.0.1:8080/gw/dbapi/banking/transaction?iban=123")
  )

  responseFuture.onComplete {
    case Success(response) =>
      response.discardEntityBytes()
      println(response)
    case Failure(exception) => println(exception)
  }
}
