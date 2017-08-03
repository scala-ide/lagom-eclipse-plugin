package org.scalaide.lagom.locator

object LagomLocatorConfiguration {
  // configuration keys
  val LagomPort = "LAGOM_PORT"
  val LagomGatewayPort = "LAGOM_GATEWAY_PORT"
  val LagomCassandraPort = "LAGOM_CASSANDRA_PORT"
  // defaults
  val LagomPortDefault = 8000.toString
  val LagomGatewayPortDefault = 9000.toString
  val LagomCassandraPortDefault = 4000.toString
  // launch facilities
  val LagomLocatorRunnerClass = "org.scalaide.lagom.locator.LagomLauncher"
  val LagomLocatorName = "lagom-locator"
  // program attributes
  val LagomPortProgArgName = "port"
  val LagomGatewayPortProgArgName = "gatewayport"
  val LagomCassandraPortProgArgName = "cassport"
}
