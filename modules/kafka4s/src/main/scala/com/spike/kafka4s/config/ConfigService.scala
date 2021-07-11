package com.spike.kafka4s.config

import cats.effect._
import cats.implicits._
import com.banno.kafka.TopicName
import com.banno.kafka.admin.AdminApi
import com.banno.kafka.schemaregistry.SchemaRegistryApi
import com.spike.common.config._
import com.spike.common.hello._
import com.spike.common.kafka._
import com.spike.kafka4s.kafka.implicits._
import com.spike.kafka4s.kafka.{Consumer, Producer}
import fs2.concurrent.Topic
import org.apache.kafka.clients.admin.NewTopic
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

trait ConfigService[F[_]] {
  def primaryHttpServer: BlazeServerBuilder[F]
  def createHelloTopic: F[TopicName]
  def createHelloConsumer(logger: Logger[F], messages: Topic[F, Option[Hello.Message]]): Resource[F, Consumer[F, Hello]]
  def createHelloProducer: Resource[F, Producer[F, Message[Hello.Id, Hello.Message]]]
  def blockingExecutionContext: Resource[F, ExecutionContextExecutorService]
}
object ConfigService {

  def impl[F[_]: ConcurrentEffect: Timer: ContextShift]: F[ConfigService[F]] =
    for {
      config <- SetupConfig.loadConfig[F]
      schemaRegistry <- SchemaRegistryApi[F](
        config.kafka.schemaRegistry.uri,
        config.kafka.schemaRegistry.cachedSchemasPerSubject)
    } yield
      new ConfigService[F] {

        def primaryHttpServer: BlazeServerBuilder[F] =
          BlazeServerBuilder[F](ExecutionContext.global).bindHttp(config.http.port, config.http.host)

        def createHelloTopic: F[TopicName] = {
          val topicName = TopicName(config.kafka.topics.hello)

          for {
            _ <- AdminApi
              .createTopicsIdempotent[F](config.kafka.server.uri, new NewTopic(topicName.toString, 1, 1.toShort) :: Nil)
              .void
            _ <- schemaRegistry.registerKey[Hello.Id](topicName.toString)
            _ <- schemaRegistry.registerValue[Hello.Message](topicName.toString)
          } yield topicName
        }

        def createHelloConsumer(
            logger: Logger[F],
            messages: Topic[F, Option[Hello.Message]]): Resource[F, Consumer[F, Hello]] =
          Consumer
            .connection[F, Hello.Id, Hello.Message](
              config.kafka.server,
              config.kafka.schemaRegistry,
              HelloClientId("hello-consumer-example"),
              HelloGroupId("hello-consumer-example-group"))
            .map(Consumer.atLeastOnce(_, TopicName(config.kafka.topics.hello), 1.second)(
              HelloConsumer.impl[F](logger, messages)))

        def createHelloProducer: Resource[F, Producer[F, Message[Hello.Id, Hello.Message]]] =
          Producer
            .connection[F](config.kafka.server, config.kafka.schemaRegistry, HelloClientId("hello-producer-example"))
            .map(Producer(_, TopicName(config.kafka.topics.hello)))

        def blockingExecutionContext: Resource[F, ExecutionContextExecutorService] =
          SetupConfig.createFixedThreadPoolExecutionContext(4)
      }

}
