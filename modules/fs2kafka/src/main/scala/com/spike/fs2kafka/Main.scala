package com.spike.fs2kafka

import cats.effect._
import com.spike.common.{Client, Server}
import com.spike.fs2kafka.config.ConfigService
import fs2.Stream

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      configService <- Stream.eval(ConfigService.impl[IO])
      client = Client.produce[IO](configService)
      app <- Server.serve[IO](configService).concurrently(client)
    } yield app).compile.drain.as(ExitCode.Success)
}
