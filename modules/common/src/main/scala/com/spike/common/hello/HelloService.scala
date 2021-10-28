package com.spike.common.hello

import cats.effect._
import org.http4s._
import org.http4s.dsl.Http4sDsl

class HelloService[F[_]: Sync] extends Http4sDsl[F] {

  def routes(messages: fs2.Stream[F, Hello.Message]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "hello" =>
        Ok(messages.map(message => s"${message.value} at ${message.timestamp}\n"))
    }

}
