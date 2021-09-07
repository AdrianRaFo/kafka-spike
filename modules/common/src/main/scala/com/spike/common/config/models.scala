package com.spike.common.config

import org.http4s.{ParseFailure, Uri}

final case class SchemaRegistry(uri: Uri, cachedSchemasPerSubject: Int)

object SchemaRegistry {
  def apply(host: String, port: Int, cachedSchemasPerSubject: Int): Either[ParseFailure, SchemaRegistry] =
    Uri.fromString(s"$host:$port").map(new SchemaRegistry(_, cachedSchemasPerSubject))
}

final case class BrokerAddress(uri: Uri)

object BrokerAddress {
  def apply(host: String, port: Int): Either[ParseFailure, BrokerAddress] =
    Uri.fromString(s"$host:$port").map(new BrokerAddress(_))
}

case class ServiceConfig(kafka: KafkaConfig, http: HttpConfig)

case class HttpConfig(host: String, port: Int)

case class TopicsConfig(hello: String)

case class KafkaConfig(server: BrokerAddress, schemaRegistry: SchemaRegistry, topics: TopicsConfig)
