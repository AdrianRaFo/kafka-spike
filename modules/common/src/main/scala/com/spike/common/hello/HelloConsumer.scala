package com.spike.common.hello

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import com.spike.common.kafka.NextMessage
import fs2.concurrent.Topic
import org.typelevel.log4cats.Logger

object HelloConsumer {

  def impl[F[_]: Sync](
      logger: Logger[F],
      output: Topic[F, Option[Hello.Message]]
  ): Kleisli[F, NextMessage[Hello.Id, Hello.Message], Hello] = Kleisli { nextMessage =>
    for {
      _ <- logger.info(s"Receiving ${nextMessage}")
      _ <- output.publish1(nextMessage.value.value.headOption.map(_ => nextMessage.value))
    } yield Hello(nextMessage.key, nextMessage.value)
  }

}
