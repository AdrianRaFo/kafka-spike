package com.spike.fs2kafka.kafka

import cats.data.Kleisli
import cats.effect._
import com.spike.common.config.BrokerAddress
import com.spike.common.hello._
import com.spike.common.kafka._
import vulcan.Codec
import fs2.kafka._
import fs2.kafka.vulcan.{avroDeserializer, AvroSettings}

import scala.concurrent.duration.FiniteDuration

object Consumer {

  def connection[F[_]: ConcurrentEffect: ContextShift: Timer, K: Codec, V: Codec, A](
      broker: BrokerAddress,
      schemaRegistrySettings: AvroSettings[F],
      clientId: HelloClientId,
      groupId: HelloGroupId,
      topicName: String,
      pollTime: FiniteDuration)(
      messageMapper: Kleisli[F, Message[K, V], A]): Resource[F, Consumer[F, A]] = {

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
        new Consumer[F, A] {
          override def deliveredMessages: fs2.Stream[F, A] =
            kafkaConsumer.stream.evalMap(it => messageMapper(Message(it.record.key, it.record.value)))
      })
  }

}
