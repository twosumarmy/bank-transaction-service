package pipeline

import akka.stream.Supervision
import io.circe.ParsingFailure
import Flows.Exceptions.RequestFlowException

object SupervisionPattern {
  object Consumer {
    val decider: Supervision.Decider = {
      case _: ParsingFailure       => Supervision.Resume
      case _: RequestFlowException => Supervision.Resume
      case _                       => Supervision.Stop
    }
  }
}
