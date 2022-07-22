package pipeline

import akka.Done
import akka.stream.scaladsl.Sink
import scala.concurrent.Future
import Events.BankingDomain._

/**
 * A collection of operators with exactly one input, requesting and accepting data elements, possibly slowing down the
 * upstream producer of elements.
 */
object Sinks {
  val outputSink: Sink[GetTransactionsEvent, Future[Done]] = Sink.foreach[GetTransactionsEvent](println)
}
