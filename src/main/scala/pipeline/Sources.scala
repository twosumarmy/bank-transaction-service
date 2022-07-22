package pipeline

import akka.kafka.{ ConsumerSettings, Subscriptions }
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Source
import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer

/**
 * A collection of operators with exactly one output, emitting data elements whenever downstream operators are ready to
 * receive them.
 */
object Sources {

  object KafkaConsumer {
    val config: Config                                     = ConfigFactory.load()
    val consumerConfig: Config                             = config.getConfig("akka.kafka.consumer")
    val consumerSettings: ConsumerSettings[String, String] =
      ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

    val consumerSource: Source[ConsumerRecord[String, String], Consumer.Control] =
      Consumer.plainSource(consumerSettings, Subscriptions.topics("banking"))
  }
}
