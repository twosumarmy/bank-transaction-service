package clients

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class AkkaHttpClient(implicit actorSystem: ActorSystem) extends HttpClient {

  private val expirationTime: FiniteDuration = 300.millis

  /**
   * Fires a single [[akka.http.scaladsl.model.HttpRequest]] with method GET across the (cached) host connection pool.
   * The [[akka.http.scaladsl.model.HttpResponse]] will be unmarshalled to the ResponseBody type B.
   */
  def get[B](uri: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map())(
    implicit ec: ExecutionContext, f: Unmarshaller[ResponseEntity, B]
  ): Future[Response[B]] = {
    processRequest[B](GET)(uri, params, headers)
  }

  private def processRequest[B](method: HttpMethod)(
    url: String,
    params: Map[String, String],
    headers: Map[String, String],
  )(implicit ec: ExecutionContext, f: Unmarshaller[ResponseEntity, B]): Future[Response[B]] = {
    val futureResponse = Http().singleRequest(
      HttpRequest(
        method = method, uri = Uri(url).withQuery(Query(params))
      ).withHeaders(parseHeaders(headers))
    )
    handleResponse(futureResponse)
  }

  private def handleResponse[B](futureResponse: Future[HttpResponse])(
    implicit ec: ExecutionContext, f: Unmarshaller[ResponseEntity, B]
  ): Future[Response[B]] = {
    futureResponse.flatMap { response =>
      if (response.status.isSuccess()) {
        validateResponse(response)
      } else {
        failureResponse(response)
      }
    }
  }

  private def validateResponse[B](response: HttpResponse)(
    implicit ec: ExecutionContext, f: Unmarshaller[ResponseEntity, B]
  ): Future[Response[B]] = {
    Unmarshal(response.entity).to[B].transform {
      case Success(body) =>
        Try(
          Response[B](
            response.status.intValue(),
            SuccessResponseBody(body),
            parseHeaders(response)
          )
        )
      case Failure(exception) =>
        Try(
          Response[B](
            response.status.intValue(),
            FailureResponseBody(s"Could not parse and validate response. (errors: ${exception.getMessage})"),
            parseHeaders(response)
          )
        )
    }
  }

  private def failureResponse[B](response: HttpResponse)(
    implicit ec: ExecutionContext
  ): Future[Response[B]] =
    response.entity.toStrict(expirationTime).map { body =>
      Response[B](
        response.status.intValue(),
        FailureResponseBody(body.toString()),
        parseHeaders(response)
      )
    }

  private def parseHeaders(headers: Map[String, String]): List[RawHeader] =
    headers.map { case (param, value) => RawHeader(param, value) }.toList

  private def parseHeaders(response: HttpResponse): Map[String, Seq[String]] =
    response.headers.foldLeft(Map[String, List[String]]()) {
      case (acc, header) =>
        acc + (header.name() -> (header.value() :: acc.getOrElse(header.name(), Nil)))
    }
}
