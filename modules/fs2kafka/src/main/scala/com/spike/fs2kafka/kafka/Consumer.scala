package com.spike.fs2kafka.kafka

import cats.effect._
import com.spike.common.config.BrokerAddress
import com.spike.common.hello._
import com.spike.common.kafka._
import fs2.kafka._

import scala.concurrent.duration.FiniteDuration

object Consumer {

  //TODO receive the vulcan deserializers
  def connection[F[_]: ConcurrentEffect: ContextShift: Timer, K: RecordDeserializer[F, *], V: RecordDeserializer[F, *]](
      broker: BrokerAddress,
      //schemaRegistry: SchemaRegistry,
      clientId: HelloClientId,
      groupId: HelloGroupId,
      topicName: String,
      pollTime: FiniteDuration) = {

    val consumerSettings = ConsumerSettings[F, K, V]
      .withBootstrapServers(broker.uri)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withClientId(clientId.value)
      .withGroupId(groupId.value)
      .withPollInterval(pollTime)

    KafkaConsumer
      .resource(consumerSettings)
      .evalTap(_.subscribeTo(topicName))
      .map(kafkaConsumer =>
        new Consumer[F, Message[K, V]] {
          override def deliveredMessages: fs2.Stream[F, Message[K, V]] =
            kafkaConsumer.stream.map(it => Message.apply(it.record.key, it.record.value))
      })
  }

}
