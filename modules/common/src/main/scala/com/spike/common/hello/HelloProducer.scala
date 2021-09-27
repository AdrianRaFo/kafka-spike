package com.spike.common.hello

import cats.effect._
import cats.implicits._
import com.spike.common.kafka.{Message, Producer}
import fs2._
import org.typelevel.log4cats.Logger

import java.util.Date
import scala.concurrent.duration._

trait HelloProducer[F[_]] {
  def sendMessages(): Stream[F, Unit]
}

object HelloProducer {

  def impl[F[_]: Async](
      producer: Producer[F, Message[Hello.Id, Hello.Message]],
      logger: Logger[F],
      sendMessagesEvery: FiniteDuration
  ): HelloProducer[F] = new HelloProducer[F] {

    private val recordsToBeWritten =
      Stream.fixedDelay(sendMessagesEvery) zipRight
        Stream.repeatEval(Async[F].delay { buildMessage(new Date()) })

    def sendMessages(): Stream[F, Unit] =
      recordsToBeWritten
        .evalTap(r => logger.info(s"[$sendMessagesEvery] $r has been sent!"))
        .evalMap(producer.sendMessage)
  }

  private def buildMessage(date: Date): Message[Hello.Id, Hello.Message] =
    Message(
      Hello.Id(date.getTime.show),
      Hello.Message(s"Hello ${nameFrom(date)}!", date.getTime)
    )

  private def nameFrom(date: Date): String = date.getTime match {
    case x if x % 15 == 0 => "Rhys"
    case x if x % 2 == 0 => "Martin"
    case x if x % 3 == 0 => "Jamie"
    case _ => "World"
  }

}
