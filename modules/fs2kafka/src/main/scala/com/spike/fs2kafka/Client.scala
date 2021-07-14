package com.spike.fs2kafka

import cats.effect._
import com.spike.common.hello.HelloProducer
import com.spike.fs2kafka.config.ConfigService
import fs2._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._

object Client {

  def produce[F[_]: ConcurrentEffect: ContextShift: Timer]: Stream[F, Unit] =
    for {
      iLogger <- Stream.eval(Slf4jLogger.create[F])
      configService <- Stream.eval(ConfigService.impl[F])
      _ <- Stream.eval(configService.createHelloTopic)
      producer <- Stream.resource(configService.createHelloProducer)
      // anotherProducer <- Stream.resource(configService.helloProducer)
      _ <- HelloProducer.impl[F](producer, iLogger, 2.seconds).sendMessages()
      // concurrently (HelloProducer.impl[F](anotherProducer, iLogger, 3.seconds).sendMessages())
    } yield ()

}
