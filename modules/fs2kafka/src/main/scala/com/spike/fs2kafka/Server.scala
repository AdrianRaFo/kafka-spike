package com.spike.fs2kafka

import cats.effect._
import com.spike.common.hello._
import com.spike.fs2kafka.config.ConfigService
import fs2._
import fs2.concurrent.Topic
import org.http4s.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Server {

  def serve[F[_]: Async]: Stream[F, ExitCode] =
    for {
      logger <- Stream.eval(Slf4jLogger.create[F])
      messages <- Stream.eval(Topic.apply[F, Option[Hello.Message]])
      configService <- Stream.eval(ConfigService.impl[F])
      _ <- Stream.eval(configService.createHelloTopic(logger))
      consumer <- Stream.resource(configService.createHelloConsumer)
      helloConsumer = new HelloConsumer(consumer, logger, messages)
      _ <- Stream.eval(logger.info("Kafka Spike Server Has Started Successfully"))
      exitCode <- configService.primaryHttpServer
        .withHttpApp(new HelloService[F].routes(messages).orNotFound)
        .serve
        .concurrently(helloConsumer.receiveHelloMessages)
    } yield exitCode

}
