package com.spike.fs2kafka.kafka

import cats.effect._
import com.spike.common.config.BrokerAddress
import com.spike.common.hello.HelloClientId
import com.spike.common.kafka.{Message, Producer}
import fs2.kafka._

object Producer {

  //TODO receive the vulcan serializers
  def connection[F[_]: ConcurrentEffect: ContextShift, K: RecordSerializer[F, *], V: RecordSerializer[F, *]](
      broker: BrokerAddress,
      clientId: HelloClientId
  ): Resource[F, KafkaProducer[F, K, V]] = {

    val producerSettings =
      ProducerSettings[F, K, V].withBootstrapServers(broker.uri).withClientId(clientId.value)

    KafkaProducer.resource(producerSettings)
  }

  def apply[F[_]: Sync, K, V](
      ): Producer[F, Message[K, V]] =
    new Producer[F, Message[K, V]] {
      def sendMessage(message: Message[K, V]): F[Unit] = Sync[F].unit //TODO
    }
}
