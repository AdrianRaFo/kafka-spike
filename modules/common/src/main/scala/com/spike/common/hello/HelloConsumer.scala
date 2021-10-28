package com.spike.common.hello

import com.spike.common.kafka.{Consumer, Message}
import fs2.Stream
import org.typelevel.log4cats.Logger

class HelloConsumer[F[_]](consumer: Consumer[F, Message[Hello.Id, Hello.Message]], logger: Logger[F]) {

  def processHelloMessages: Stream[F, Hello.Message] =
    for {
      message <- consumer.deliveredMessages
      _ <- Stream.eval(logger.info(s"Receiving $message"))
    } yield message.value

}
