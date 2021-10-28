package com.spike.common

import cats.effect.{Async, ExitCode}
import com.spike.common.config.ConfigService
import com.spike.common.hello.{HelloConsumer, HelloService}
import fs2.Stream
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Server {

  def serve[F[_]: Async](configService: ConfigService[F]): Stream[F, ExitCode] =
    for {
      logger <- Stream.eval(Slf4jLogger.create[F])
      _ <- Stream.eval(configService.createHelloTopic(logger))
      consumer <- Stream.resource(configService.createHelloConsumer)
      _ <- Stream.eval(logger.info("Kafka Spike Server Has Started Successfully"))
      helloConsumer = new HelloConsumer(consumer, logger)
      exitCode <- configService.primaryHttpServer
        .withHttpApp(new HelloService[F].routes(helloConsumer.processHelloMessages).orNotFound)
        .serve
    } yield exitCode

}
