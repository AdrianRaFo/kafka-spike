package com.spike.fs2kafka.config

import cats.effect._
import cats.implicits._
import com.spike.common.config._
import com.spike.common.hello._
import com.spike.common.kafka._
import com.spike.fs2kafka.kafka.{Consumer, Producer}
import fs2.concurrent.Topic
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings}
import fs2.kafka.{AdminClientSettings, KafkaAdminClient}
import org.apache.kafka.clients.admin.NewTopic
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object ConfigService {

  def impl[F[_]: ConcurrentEffect: Timer: ContextShift]: F[ConfigService[F]] =
    for {
      config <- SetupConfig.loadConfig[F]
      _ = AvroSettings {
        SchemaRegistryClientSettings[IO](config.kafka.schemaRegistry.uri)
          .withMaxCacheSize(config.kafka.schemaRegistry.cachedSchemasPerSubject)
      }
    } yield
      new ConfigService[F] {

        def primaryHttpServer: BlazeServerBuilder[F] =
          BlazeServerBuilder[F](ExecutionContext.global).bindHttp(config.http.port, config.http.host)

        def createHelloTopic: F[String] = {
          val adminClientSettings: AdminClientSettings[F] =
            AdminClientSettings[F].withBootstrapServers(config.kafka.server.uri)

          KafkaAdminClient.resource(adminClientSettings).use {
            _.createTopic(new NewTopic(config.kafka.topics.hello, 1, 1.toShort)).as(config.kafka.topics.hello)
          }
        }

        def createHelloConsumer(
            logger: Logger[F],
            messages: Topic[F, Option[Hello.Message]]): Resource[F, Consumer[F, Hello]] =
          Consumer
            .connection[F, Hello.Id, Hello.Message](
              config.kafka.server,
              //config.kafka.schemaRegistry,
              HelloClientId("hello-consumer-example"),
              HelloGroupId("hello-consumer-example-group"),
              config.kafka.topics.hello,
              1.second
            )

        def createHelloProducer: Resource[F, Producer[F, Message[Hello.Id, Hello.Message]]] =
          Producer
            .connection[F, Hello.Id, Hello.Message](config.kafka.server, HelloClientId("hello-producer-example"))
            .map(Producer())

      }

}
