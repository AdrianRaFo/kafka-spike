package com.spike.common

import cats.effect.Async
import com.spike.common.config.ConfigService
import com.spike.common.hello.HelloProducer
import fs2.Stream
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.DurationInt

object Client {

  def produce[F[_]: Async](configService: ConfigService[F]): Stream[F, Unit] =
    for {
      logger <- Stream.eval(Slf4jLogger.create[F])
      _ <- Stream.eval(configService.createHelloTopic(logger))
      producer <- Stream.resource(configService.createHelloProducer)
      // anotherProducer <- Stream.resource(configService.helloProducer)
      _ <- HelloProducer.impl[F](producer, logger, 2.seconds).sendMessages()
      // concurrently (HelloProducer.impl[F](anotherProducer, iLogger, 3.seconds).sendMessages())
    } yield ()

}
