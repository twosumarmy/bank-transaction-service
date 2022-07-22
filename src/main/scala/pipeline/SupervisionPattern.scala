package pipeline

import akka.stream.Supervision
import io.circe.ParsingFailure

object SupervisionPattern {
  object Consumer {
    val decider: Supervision.Decider = {
      case _: ParsingFailure => Supervision.Resume
      case _                 => Supervision.Stop
    }
  }
}
