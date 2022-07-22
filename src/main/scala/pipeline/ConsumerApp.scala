package pipeline

import akka.actor.ActorSystem
import akka.stream.{ ActorAttributes, ActorMaterializer, Materializer }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import scala.util.{ Failure, Success }

object ConsumerApp extends App with FailFastCirceSupport {

  implicit val system: ActorSystem        = ActorSystem("consumerActor")
  implicit val materializer: Materializer = ActorMaterializer()
  import system.dispatcher

  import Flows._
  import Sources.KafkaConsumer._
  import Sinks._
  import SupervisionPattern.Consumer._

  val consume = consumerSource
    /**
     * Decodes ConsumerRecord to an event called GetTransactionsEvent. According to the given super vision strategy the
     * stream will be resumed and the current element will be dropped if a ParsingFailure occurs.
     */
    .via(
      decodingFlow
        .withAttributes(ActorAttributes.supervisionStrategy(decider))
        .async
    )
    .runWith(outputSink)

  consume.onComplete {
    case Success(_)   => println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }

}
