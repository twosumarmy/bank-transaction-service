package clients

import akka.http.scaladsl.model.ResponseEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Trait with standard REST operations, with parametrized methods, where B is clients.ResponseBody type.
 */
trait HttpClient {
  def get[B](uri: String, params: Map[String, String] = Map(), headers: Map[String, String] = Map())(implicit
    ec: ExecutionContext,
    f: Unmarshaller[ResponseEntity, B]
  ): Future[Response[B]]
}

/**
 * Contains response data with unmarshalled Json
 *
 * @param status
 *   http status code
 * @param body
 *   wrapper class contains unmarshalled object of type
 * @param headers
 *   Map of http headers
 * @tparam B
 *   type of object you unmarshall to
 */
case class Response[B](status: Int, body: ResponseBody[B], headers: Map[String, Seq[String]])

/**
 * Wrapper trait, contains response body of defined type
 *
 * @tparam B
 *   clients.Response type
 */
sealed trait ResponseBody[B]

/**
 * Positive projection of [[ResponseBody]], describe a wrapper object for successfully unmarshalled(parsed from json and
 * validated) object instance
 *
 * @param bodyObject
 * @tparam B
 *   Type which response contains
 */
final case class SuccessResponseBody[B](bodyObject: B) extends ResponseBody[B]

/**
 * Negative projection of [[ResponseBody]], with rejection info, such as unsuccessful status code or json validation
 * error
 *
 * @param error
 * @tparam B
 *   Type which response contains
 */
final case class FailureResponseBody[B](error: String) extends ResponseBody[B]
