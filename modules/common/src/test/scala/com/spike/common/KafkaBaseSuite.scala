package com.spike.common

import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.munit.TestContainersForAll
import com.dimafeng.testcontainers.{KafkaContainer, SchemaRegistryContainer}
import com.spike.common.config.BrokerAddress
import munit.CatsEffectSuite
import org.testcontainers.containers.Network

import scala.jdk.CollectionConverters._

trait KafkaBaseSuite extends CatsEffectSuite with TestContainersForAll {

  override type Containers = KafkaContainer and SchemaRegistryContainer

  //this should be the same version that your lib is using under the hood
  val kafkaVersion = "6.1.1"

  //these are the default kafka host name but because that may change
  //we need to ensure that these are the values for container network, kafka and the schema registry
  val brokerId = 1
  val hostName = s"kafka$brokerId"

  def getKafkaAddress: BrokerAddress = withContainers {
    case kafkaContainer and _ => BrokerAddress(kafkaContainer.bootstrapServers)
  }

  def getSchemaRegistryAddress: String = withContainers {
    case _ and schemaRegistryContainer => schemaRegistryContainer.schemaUrl
  }

  override def startContainers(): Containers = {
    //a way to communicate containers
    val network: Network = Network.newNetwork()

    val kafkaContainer: KafkaContainer = KafkaContainer.Def(kafkaVersion).createContainer()
    kafkaContainer.container
      .withNetwork(network)
      .withNetworkAliases(hostName)
      .withEnv(
        Map[String, String](
          "KAFKA_BROKER_ID" -> brokerId.toString,
          "KAFKA_HOST_NAME" -> hostName,
          "KAFKA_AUTO_CREATE_TOPICS_ENABLE" -> "false"
        ).asJava
      )
    kafkaContainer.start

    val schemaRegistryContainer: SchemaRegistryContainer =
      SchemaRegistryContainer.Def(network, hostName, kafkaVersion).start()

    kafkaContainer and schemaRegistryContainer
  }

}
