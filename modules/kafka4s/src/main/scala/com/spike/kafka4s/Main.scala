package com.spike.kafka4s

import cats.effect._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    Server.serve[IO].concurrently(Client.produce[IO]).compile.drain.as(ExitCode.Success)
}
