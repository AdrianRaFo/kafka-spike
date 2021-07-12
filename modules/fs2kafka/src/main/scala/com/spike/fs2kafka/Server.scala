package com.spike.fs2kafka

import cats.effect._
import com.spike.common.hello._
import com.spike.fs2kafka.config.ConfigService
import fs2._
import fs2.concurrent.Topic
import org.http4s.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Server {

  def serve[F[_]: ConcurrentEffect: ContextShift: Timer]: Stream[F, ExitCode] =
    for {
      iLogger <- Stream.eval(Slf4jLogger.create[F])
      messages <- Stream.eval(Topic.apply[F, Option[Hello.Message]](None))
      configService <- Stream.eval(ConfigService.impl[F])
      _ <- Stream.eval(configService.createHelloTopic)
      consumer <- Stream.resource(configService.createHelloConsumer(iLogger, messages))
      _ <- Stream.eval(iLogger.info("Kafka Spike Server Has Started Successfully"))
      exitCode <- configService.primaryHttpServer
        .withHttpApp(new HelloService[F].routes(messages).orNotFound)
        .serve
        .concurrently(consumer.deliveredMessages)
    } yield exitCode

}
