package com.spike.common.config

import cats.effect.Resource
import com.spike.common.hello.Hello
import com.spike.common.kafka.{Consumer, Message, Producer}
import fs2.concurrent.Topic
import org.http4s.server.blaze.BlazeServerBuilder
import org.typelevel.log4cats.Logger

trait ConfigService[F[_]] {
  def primaryHttpServer: BlazeServerBuilder[F]
  def createHelloTopic: F[String]
  def createHelloConsumer(logger: Logger[F], messages: Topic[F, Option[Hello.Message]]): Resource[F, Consumer[F, Hello]]
  def createHelloProducer: Resource[F, Producer[F, Message[Hello.Id, Hello.Message]]]
}
