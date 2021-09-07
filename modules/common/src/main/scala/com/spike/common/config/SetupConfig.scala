package com.spike.common.config

import cats.effect._
import com.typesafe.config.ConfigFactory
import org.http4s.Uri
import pureconfig._
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._

import java.util.concurrent._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

object SetupConfig {

  //pureconfig-http4s had binary incompatibilities
  implicit val uriReader: ConfigReader[Uri] =
    ConfigReader.fromString(str =>
      Uri.fromString(str).fold(err => Left(CannotConvert(str, "Uri", err.sanitized)), uri => Right(uri))
    )

  def loadConfig[F[_]: Sync]: F[ServiceConfig] =
    Sync[F].delay(ConfigSource.fromConfig(ConfigFactory.defaultApplication()).loadOrThrow[ServiceConfig])

  def createFixedThreadPoolExecutionContext[F[_]: Sync](
      numberThreads: Int
  ): Resource[F, ExecutionContextExecutorService] =
    Resource.make[F, ExecutionContextExecutorService](
      Sync[F].delay(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(numberThreads)))
    )(r => Sync[F].delay(r.shutdown()))

}
