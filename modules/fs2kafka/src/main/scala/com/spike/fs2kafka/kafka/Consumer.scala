package com.spike.fs2kafka.kafka

import cats.effect._
import com.spike.common.config.BrokerAddress
import com.spike.common.hello._
import com.spike.common.kafka._
import vulcan.Codec
import fs2.kafka._
import fs2.kafka.vulcan.{avroDeserializer, AvroSettings}

import scala.concurrent.duration.FiniteDuration

object Consumer {

  def connection[F[_]: Async, K: Codec, V: Codec](
      broker: BrokerAddress,
      schemaRegistrySettings: AvroSettings[F],
      clientId: HelloClientId,
      groupId: HelloGroupId,
      topicName: String,
      pollTime: FiniteDuration): Resource[F, Consumer[F, Message[K, V]]] = {

    val consumerSettings =
      ConsumerSettings[F, K, V](
        avroDeserializer[K].using(schemaRegistrySettings),
        avroDeserializer[V].using(schemaRegistrySettings))
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
            kafkaConsumer.stream.map(it => Message(it.record.key, it.record.value))
      })
  }

}
