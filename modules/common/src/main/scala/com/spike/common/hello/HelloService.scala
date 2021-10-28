package com.spike.common.hello

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger

class HelloService[F[_]: Sync](logger: Logger[F]) extends Http4sDsl[F] {

  def routes(messages: fs2.Stream[F, Hello.Message]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "hello" =>
        Ok(messages.evalMap(message =>
          logger.info(s"Receiving $message").as(s"${message.value} at ${message.timestamp}\n")))
    }

}
