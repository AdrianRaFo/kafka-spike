package com.spike.common

import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.{GenericContainer, KafkaContainer}
import com.dimafeng.testcontainers.munit.TestContainersForAll
import com.spike.common.config.BrokerAddress
import munit.CatsEffectSuite
import org.http4s.Uri
import org.testcontainers.containers.Network

import scala.jdk.CollectionConverters._

trait KafkaBaseSuite extends CatsEffectSuite with TestContainersForAll {

  override type Containers = KafkaContainer and GenericContainer

  //this should be the same version that your lib is using under the hood
  val kafkaVersion = "6.1.1"

  def getKafkaAddress: BrokerAddress = withContainers {
    case kafkaContainer and _ => BrokerAddress(Uri.unsafeFromString(kafkaContainer.bootstrapServers))
  }

  def getSchemaRegistryAddress: String = withContainers {
    case _ and schemaRegistryContainer =>
      s"http://${schemaRegistryContainer.container.getHost}:${schemaRegistryContainer.container.getMappedPort(SchemaRegistryContainer.defaultSchemaPort)}"
  }

  override def startContainers(): Containers = {
    val network: Network = Network.newNetwork()

    val kafkaContainer: KafkaContainer = KafkaContainer.Def(kafkaVersion).createContainer()
    kafkaContainer.container
      .withNetwork(network)
      .withNetworkAliases(hostName)
      .withEnv(
        Map[String, String](
          "KAFKA_AUTO_CREATE_TOPICS_ENABLE" -> "false" //there is some issue with this setting
        ).asJava
      )
    kafkaContainer.start

    val schemaRegistryContainer: GenericContainer = SchemaRegistryContainer.Def(network, hostName, kafkaVersion).start()
    kafkaContainer and schemaRegistryContainer
  }

}