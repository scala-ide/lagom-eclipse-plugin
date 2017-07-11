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

  val port = args.find(_.startsWith(LagomPortProgArgName)).map(_.drop(LagomPortProgArgName.length)).get.toInt
  val gateway = args.find(_.startsWith(LagomGatewayPortProgArgName)).map(_.drop(LagomGatewayPortProgArgName.length)).get.toInt
  val cassandraPort = args.find(_.startsWith(LagomCassandraPortProgArgName)).map(_.drop(LagomCassandraPortProgArgName.length)).get

  val httpHostname = "http://127.0.0.1"

  try {
    import scala.collection.JavaConverters._
    val serLoc = new ServiceLocatorServer()
    serLoc.start(port, gateway, Map("cas_native" -> s"$httpHostname:$cassandraPort/cas_native").asJava)
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
