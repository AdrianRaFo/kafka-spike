package com.spike.fs2kafka.config

import cats.effect._
import cats.implicits._
import com.spike.common.config._
import com.spike.common.hello._
import com.spike.common.kafka._
import com.spike.fs2kafka.kafka.{Consumer, Producer}
import com.spike.fs2kafka.kafka.implicits._
import fs2.kafka.vulcan.{AvroSettings, SchemaRegistryClientSettings}
import fs2.kafka.{AdminClientSettings, KafkaAdminClient}
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.errors.TopicExistsException
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object ConfigService {

  def impl[F[_]: Async]: F[ConfigService[F]] =
    for {
      config <- SetupConfig.loadConfig[F]
      schemaRegistry <- SchemaRegistryClientSettings[F](config.kafka.schemaRegistry.uri)
        .withMaxCacheSize(config.kafka.schemaRegistry.cachedSchemasPerSubject)
        .createSchemaRegistryClient
        .map(AvroSettings(_))
    } yield
      new ConfigService[F] {

        def primaryHttpServer: BlazeServerBuilder[F] =
          BlazeServerBuilder[F](ExecutionContext.global).bindHttp(config.http.port, config.http.host)

        def createHelloTopic(logger: Logger[F]): F[String] = {
          val adminClientSettings: AdminClientSettings = AdminClientSettings(config.kafka.server.uri)

          KafkaAdminClient.resource(adminClientSettings).use {
            _.createTopic(new NewTopic(config.kafka.topics.hello, 1, 1.toShort))
              .recoverWith {
                case _: TopicExistsException => logger.info(s"Topic ${config.kafka.topics.hello} already exists")
              }
              .as(config.kafka.topics.hello)
          }
        }

        def createHelloConsumer: Resource[F, Consumer[F, Message[Hello.Id, Hello.Message]]] =
          Consumer
            .connection[F, Hello.Id, Hello.Message](
              config.kafka.server,
              schemaRegistry,
              SetupConfig.createKafkaClientId("hello-consumer-example"),
              HelloGroupId("hello-consumer-example-group"),
              config.kafka.topics.hello,
              1.second
            )

        def createHelloProducer: Resource[F, Producer[F, Message[Hello.Id, Hello.Message]]] =
          Producer
            .connection[F, Hello.Id, Hello.Message](
              config.kafka.server,
              schemaRegistry,
              SetupConfig.createKafkaClientId("hello-producer-example"),
              config.kafka.topics.hello,
            )

      }

}
