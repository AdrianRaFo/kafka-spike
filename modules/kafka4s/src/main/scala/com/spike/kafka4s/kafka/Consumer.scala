package com.spike.kafka4s.kafka

import cats.data.Kleisli
import cats.effect._
import com.banno.kafka._
import com.banno.kafka.consumer._
import com.sksamuel.avro4s.FromRecord
import com.spike.common.config.{BrokerAddress, SchemaRegistry}
import com.spike.common.hello._
import com.spike.common.kafka._
import fs2._
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.duration._

object Consumer {

  final case class Connection[F[_], K, V] private (raw: ConsumerApi[F, K, V]) extends AnyVal

  private[this] def toNextMessage[K, V]: ConsumerRecord[K, V] => NextMessage[K, V] =
    x => NextMessage[K, V](x.key(), x.value())

  def connection[F[_]: Async: ContextShift, K: FromRecord, V: FromRecord](
    broker: BrokerAddress,
    schemaRegistry: SchemaRegistry,
    clientId: HelloClientId,
    groupId: HelloGroupId,
    autocommit: Boolean = false): Resource[F, Connection[F, K, V]] =
    ConsumerApi.Avro4s
      .resource[F, K, V](
        BootstrapServers(broker.uri),
        SchemaRegistryUrl(schemaRegistry.uri),
        ClientId(clientId.value),
        GroupId(groupId.value),
        EnableAutoCommit(autocommit)
      )
      .map(Connection.apply)

  def atMostOnce[F[_], K, V](
      connection: Connection[F, K, V],
      topicName: TopicName,
      pollTime: FiniteDuration): Consumer[F, NextMessage[K, V]] =
    new Consumer[F, NextMessage[K, V]] {
      def deliveredMessages: Stream[F, NextMessage[K, V]] =
        Stream
          .eval(connection.raw.subscribe(topicName.toString)) >> connection.raw
          .recordStream(pollTime)
          .map(toNextMessage)
    }

  def atLeastOnce[F[_]: Sync, K, V, A](connection: Connection[F, K, V], topicName: TopicName, pollTime: FiniteDuration)(
      handle: Kleisli[F, NextMessage[K, V], A]): Consumer[F, A] =
    new Consumer[F, A] {
      def deliveredMessages: Stream[F, A] =
        Stream
          .eval(connection.raw.subscribe(topicName.toString)) >> connection.raw
          .readProcessCommit(pollTime)(x => handle(toNextMessage(x)))
    }

}