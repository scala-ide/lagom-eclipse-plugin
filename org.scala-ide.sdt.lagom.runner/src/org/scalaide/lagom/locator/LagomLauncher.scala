package org.scalaide.lagom.locator

import scala.util.Try

import com.lightbend.lagom.discovery.ServiceLocatorServer

object LagomLauncher extends App {
  /**
   * Program attributes.
   * Keep in sync with [[org.scalaide.lagom.locator.LagomLocatorConfiguration]]
   */
  val LagomPortProgArgName = "port"
  val LagomGatewayPortProgArgName = "gatewayport"
  val LagomCassandraPortProgArgName = "cassport"
  val LagomKafkaPortProgArgName = "kafkaport"

  val port = args.find(_.startsWith(LagomPortProgArgName)).map(_.drop(LagomPortProgArgName.length)).get.toInt
  val gateway = args.find(_.startsWith(LagomGatewayPortProgArgName)).map(_.drop(LagomGatewayPortProgArgName.length)).get.toInt
  val cassandraPort = args.find(_.startsWith(LagomCassandraPortProgArgName)).map(_.drop(LagomCassandraPortProgArgName.length))
  val kafkaPort = args.find(_.startsWith(LagomKafkaPortProgArgName)).map(_.drop(LagomKafkaPortProgArgName.length))

  val tcpHostname = "tcp://127.0.0.1"

  try {
    import scala.collection.JavaConverters._
    val serLoc = new ServiceLocatorServer()
    val cassLocation = cassandraPort.fold(Map.empty[String, String])(port => Map("cas_native" -> s"$tcpHostname:$port/cas_native"))
    val kafkLocation = kafkaPort.fold(Map.empty[String, String])(port => Map("kafka_native" -> s"$tcpHostname:$port/kafka_native"))
    serLoc.start(port, gateway, (cassLocation ++ kafkLocation).asJava)
    Runtime.getRuntime.addShutdownHook {
      new Thread {
        override def run = {
          Try(serLoc.close())
        }
      }
    }
  } catch {
    case e: Throwable => e.printStackTrace()
  }
}
