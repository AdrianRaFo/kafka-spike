package com.spike.common.hello

import cats.effect._
import fs2.concurrent.Topic
import org.http4s._
import org.http4s.dsl.Http4sDsl

class HelloService[F[_]: Sync] extends Http4sDsl[F] {

  def routes(
      messages: Topic[F, Option[Hello.Message]]
  ): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "hello" =>
        Ok(messages.subscribe(10).collect { case Some(i) => i }.map(x => s"${x.value} at ${x.timestamp}\n"))
    }

}
