package com.spike.fs2kafka.kafka

import cats.effect._
import cats.implicits._
import com.spike.common.config.BrokerAddress
import com.spike.common.hello.HelloClientId
import com.spike.common.kafka.{Message, Producer}
import vulcan.Codec
import fs2.kafka._
import fs2.kafka.vulcan.{avroSerializer, AvroSettings}

object Producer {

  def connection[F[_]: Async, K: Codec, V: Codec](
      broker: BrokerAddress,
      schemaRegistrySettings: AvroSettings[F],
      clientId: HelloClientId,
      topic: String
  ): Resource[F, Producer[F, Message[K, V]]] = {

    val producerSettings =
      ProducerSettings[F, K, V](
        avroSerializer[K].using(schemaRegistrySettings),
        avroSerializer[V].using(schemaRegistrySettings))
        .withBootstrapServers(broker.uri)
        .withClientId(clientId.value)

    KafkaProducer
      .resource(producerSettings)
      .map(kafkaProducer =>
        new Producer[F, Message[K, V]] {
          def sendMessage(message: Message[K, V]): F[Unit] =
            kafkaProducer.produce(ProducerRecords.one(ProducerRecord(topic, message.key, message.value))).flatten.void
      })
  }

}
