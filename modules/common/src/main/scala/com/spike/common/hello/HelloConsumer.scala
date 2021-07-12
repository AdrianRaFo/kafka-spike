package com.spike.common.hello

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import com.spike.common.kafka.Message
import fs2.concurrent.Topic
import org.typelevel.log4cats.Logger

object HelloConsumer {

  def impl[F[_]: Sync](
      logger: Logger[F],
      output: Topic[F, Option[Hello.Message]]
  ): Kleisli[F, Message[Hello.Id, Hello.Message], Hello] = Kleisli { Message =>
    for {
      _ <- logger.info(s"Receiving ${Message}")
      _ <- output.publish1(Message.value.value.headOption.map(_ => Message.value))
    } yield Hello(Message.key, Message.value)
  }

}
