package com.spike.fs2kafka.kafka

import cats.effect._
import com.spike.common.config.{BrokerAddress, SchemaRegistry}
import com.spike.common.hello._
import com.spike.common.kafka._
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, KafkaConsumer}
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.duration.FiniteDuration

object Consumer {

  def connection[F[_]: ConcurrentEffect: ContextShift: Timer, K, V, A](
      broker: BrokerAddress,
      //schemaRegistry: SchemaRegistry,
      clientId: HelloClientId,
      groupId: HelloGroupId,
      topicName: String,
      pollTime: FiniteDuration) = {

    val consumerSettings = ConsumerSettings[F, String, String]
      .withBootstrapServers(broker.uri)
      .withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withClientId(clientId.value)
      .withGroupId(groupId.value)
      .withPollInterval(pollTime)

    KafkaConsumer.resource(consumerSettings).evalTap(_.subscribeTo(topicName)).map(kafkaConsumer =>
    new Consumer[F, A]{
      override def deliveredMessages: fs2.Stream[F, A] = kafkaConsumer.stream.map(_.record.)
    })
  }

}
