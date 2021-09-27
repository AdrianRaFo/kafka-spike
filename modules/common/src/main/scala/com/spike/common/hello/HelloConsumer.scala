package com.spike.common.hello

import com.spike.common.kafka.{Consumer, Message}
import fs2.Stream
import fs2.concurrent.Topic
import org.typelevel.log4cats.Logger

class HelloConsumer[F[_]](
    consumer: Consumer[F, Message[Hello.Id, Hello.Message]],
    logger: Logger[F],
    output: Topic[F, Option[Hello.Message]]) {

  def receiveHelloMessages: Stream[F, Unit] =
    for {
      message <- consumer.deliveredMessages
      _ <- Stream.eval(logger.info(s"Receiving $message"))
      _ <- Stream.eval(output.publish1(message.value.value.headOption.map(_ => message.value)))
    } yield ()

}
