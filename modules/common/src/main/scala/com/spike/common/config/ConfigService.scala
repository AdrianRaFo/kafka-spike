package com.spike.common.config

import cats.effect.Resource
import com.spike.common.hello.Hello
import com.spike.common.kafka.{Consumer, Message, Producer}
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.Logger

trait ConfigService[F[_]] {
  def primaryHttpServer: BlazeServerBuilder[F]
  def createHelloTopic(logger: Logger[F]): F[String]
  def createHelloConsumer: Resource[F, Consumer[F, Message[Hello.Id, Hello.Message]]]
  def createHelloProducer: Resource[F, Producer[F, Message[Hello.Id, Hello.Message]]]
}
