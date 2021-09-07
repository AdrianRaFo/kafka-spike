package com.spike.common

import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.implicits.catsSyntaxApplicativeError
import com.spike.common.hello._
import com.spike.common.kafka.Message
import com.spike.fs2kafka.kafka.implicits._
import com.spike.fs2kafka.kafka.{Consumer, Producer}
import fs2.Stream
import fs2.kafka.vulcan._
import fs2.kafka.{AdminClientSettings, KafkaAdminClient}
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class KafkaSpec extends KafkaBaseSuite {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  implicit val t: Timer[IO] = IO.timer(ExecutionContext.global)
  val topic = "topic"
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  def createTopic: IO[Unit] = {
    val adminClientSettings: AdminClientSettings[IO] =
      AdminClientSettings[IO].withBootstrapServers(getKafkaAddress.uri.toString())

    KafkaAdminClient.resource(adminClientSettings).use {
      _.createTopic(new NewTopic(topic, 1, 1.toShort)).recoverWith {
        case _: TopicExistsException => logger.info(s"Topic $topic already exists")
      }
    }
  }

  test("Producer and Consumer worked using kafka and schema registry from containers") {

    val expectedMessage: Message[Hello.Id, Hello.Message] = Message(
      Hello.Id("123"),
      Hello.Message(
        "Hello 1",
        1
      )
    )

    val avroSettings: AvroSettings[IO] =
      AvroSettings {
        SchemaRegistryClientSettings[IO](getSchemaRegistryAddress)
          .withAuth(Auth.Basic("user", "password"))
      }

    val res = for {
      _ <- Stream.eval(createTopic)
      producer <- Stream.resource(
        Producer.connection[IO, Hello.Id, Hello.Message](
          getKafkaAddress,
          avroSettings,
          HelloClientId("helloProducer"),
          topic
        )
      )
      _ <- Stream.eval(producer.sendMessage(expectedMessage))
      consumer <- Stream.resource(
        Consumer.connection[IO, Hello.Id, Hello.Message](
          getKafkaAddress,
          avroSettings,
          HelloClientId("helloConsumer"),
          HelloGroupId("spike"),
          topic,
          1.second
        )
      )
      message <- consumer.deliveredMessages.take(1)
    } yield message

    assertIO(res.compile.lastOrError, expectedMessage)
  }

}
