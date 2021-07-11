package com.spike.kafka4s.kafka

import cats.effect._
import cats.implicits._
import com.banno.kafka._
import com.banno.kafka.producer._
import com.sksamuel.avro4s.ToRecord
import com.spike.common.config.{BrokerAddress, SchemaRegistry}
import com.spike.common.hello.HelloClientId
import com.spike.common.kafka.{Message, Producer}
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerRecord

object Producer {

  final case class Connection[F[_]] private (raw: ProducerApi[F, GenericRecord, GenericRecord]) extends AnyVal

  def connection[F[_]: Async](
      broker: BrokerAddress,
      schemaRegistry: SchemaRegistry,
      clientId: HelloClientId
  ): Resource[F, Connection[F]] =
    ProducerApi.Avro.Generic
      .resource[F](
        BootstrapServers(broker.uri),
        SchemaRegistryUrl(schemaRegistry.uri),
        ClientId(clientId.value)
      )
      .map(Connection.apply)

  def apply[F[_]: Sync, K: ToRecord, V: ToRecord](
      connection: Connection[F],
      topicName: TopicName
  ): Producer[F, Message[K, V]] =
    new Producer[F, Message[K, V]] {
      val base = connection.raw.toAvro4s[K, V]
      def sendMessage(message: Message[K, V]): F[Unit] =
        base.sendSync(new ProducerRecord(topicName.toString, message.key, message.value)).void
    }
}
