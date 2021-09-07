package com.spike.common

import com.dimafeng.testcontainers.{ContainerDef, GenericContainer}
import org.testcontainers.containers.Network

import scala.jdk.CollectionConverters._

class SchemaRegistryContainer(network: Network, kafkaHost: String, confluentPlatformVersion: String, schemaPort: Int)
    extends GenericContainer(
      dockerImage = s"confluentinc/cp-schema-registry:$confluentPlatformVersion",
      exposedPorts = Seq(schemaPort),
    ) {

  container.withNetwork(network)
  container.setEnv(
    List(
      s"SCHEMA_REGISTRY_HOST_NAME=${container.getHost}",
      s"SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS=$kafkaHost:9092"
    ).asJava)

  def schemaUrl: String =
    s"http://${container.getHost}:${container.getMappedPort(schemaPort)}"

}

object SchemaRegistryContainer {

  val defaultSchemaPort = 8081

  case class Def(
      network: Network,
      kafkaHost: String,
      confluentPlatformVersion: String,
      schemaPort: Int = defaultSchemaPort)
      extends ContainerDef {

    override type Container = SchemaRegistryContainer

    override def createContainer(): SchemaRegistryContainer =
      new SchemaRegistryContainer(network, kafkaHost, confluentPlatformVersion, schemaPort)
  }
}
