package pipeline

import akka.Done
import akka.stream.scaladsl.Sink
import scala.concurrent.Future
import accounts.db.DeutscheBankAccount.Transaction

/**
 * A collection of operators with exactly one input, requesting and accepting data elements, possibly slowing down the
 * upstream producer of elements.
 */
object Sinks {
  // TODO: produce a message for kafka
  val outputSink: Sink[Future[Transaction], Future[Done]] = Sink.foreach[Future[Transaction]](println)
}
