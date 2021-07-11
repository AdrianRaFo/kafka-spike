package com.spike.common.config

final case class SchemaRegistry(host: String, port: Int, cachedSchemasPerSubject: Int) {
  def uri = s"http://$host:$port"
}

final case class BrokerAddress(host: String, port: Int) {
  def uri = s"http://$host:$port"
}
case class ServiceConfig(kafka: KafkaConfig, http: HttpConfig)

case class HttpConfig(host: String, port: Int)

case class TopicsConfig(hello: String)

case class KafkaConfig(server: BrokerAddress, schemaRegistry: SchemaRegistry, topics: TopicsConfig)
