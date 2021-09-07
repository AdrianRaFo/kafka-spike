package com.spike.common.config

final case class SchemaRegistry(uri: String, cachedSchemasPerSubject: Int)

final case class BrokerAddress(uri: String)

case class ServiceConfig(kafka: KafkaConfig, http: HttpConfig)

case class HttpConfig(host: String, port: Int)

case class TopicsConfig(hello: String)

case class KafkaConfig(server: BrokerAddress, schemaRegistry: SchemaRegistry, topics: TopicsConfig)
