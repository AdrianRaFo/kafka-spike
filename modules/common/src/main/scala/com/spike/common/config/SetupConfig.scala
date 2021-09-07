package com.spike.common.config

import cats.effect._
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.http4s._

import java.util.concurrent._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

object SetupConfig {

  def loadConfig[F[_]: Sync]: F[ServiceConfig] =
    Sync[F].delay(ConfigSource.fromConfig(ConfigFactory.defaultApplication()).loadOrThrow[ServiceConfig])

  def createFixedThreadPoolExecutionContext[F[_]: Sync](
      numberThreads: Int
  ): Resource[F, ExecutionContextExecutorService] =
    Resource.make[F, ExecutionContextExecutorService](
      Sync[F].delay(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numberThreads)))
    )(r => Sync[F].delay(r.shutdown()))

}
