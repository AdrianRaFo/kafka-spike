package com.spike.fs2kafka.kafka

import cats.effect._
import com.spike.common.config.{BrokerAddress, SchemaRegistry}
import com.spike.common.hello.HelloClientId
import com.spike.common.kafka.{Message, Producer}
import fs2.kafka._

object Producer {

  def connection[F[_]: ConcurrentEffect: ContextShift](
      broker: BrokerAddress,
      schemaRegistry: SchemaRegistry,
      clientId: HelloClientId
  ): Resource[F, KafkaProducer[F, String, String]] = {

    val producerSettings =
      ProducerSettings[F, String, String].withBootstrapServers(broker.uri).withClientId(clientId.value)

    KafkaProducer.resource(producerSettings)
  }

  def apply[F[_]: Sync, K, V](
      connection: Connection[F],
      topicName: String
  ): Producer[F, Message[K, V]] =
    new Producer[F, Message[K, V]] {
      val base = connection.raw.toAvro4s[K, V]
      def sendMessage(message: Message[K, V]): F[Unit] =
        base.sendSync(new ProducerRecord(topicName.toString, message.key, message.value)).void
    }
}
