package com.spike.common.config

import cats.effect._
import com.spike.common.hello.HelloClientId
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.generic.auto._

import java.util.UUID

object SetupConfig {

  def loadConfig[F[_]: Sync]: F[ServiceConfig] =
    Sync[F].delay(ConfigSource.fromConfig(ConfigFactory.defaultApplication()).loadOrThrow[ServiceConfig])

  def createKafkaClientId(appName: String): HelloClientId = HelloClientId(appName + UUID.randomUUID())

}
