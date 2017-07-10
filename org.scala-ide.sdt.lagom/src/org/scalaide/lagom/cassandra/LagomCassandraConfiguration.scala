package org.scalaide.lagom.cassandra

object LagomCassandraConfiguration {
  // configuration keys
  val LagomPort = "LAGOM_CASSANDRA_PORT"
  val LagomTimeout = "LAGOM_STARTUP_TIMEOUT"
  // defaults
  val LagomPortDefault = 4000.toString
  val LagomTimeoutDefault = 2000.toString
  val NotSet = ""
  // launch facilities
  val LagomCassandraRunnerClass = "org.scalaide.lagom.cassandra.LagomLauncher"
  val LagomCassandraName = "lagom-cassandra"
  // program attributes
  val LagomPortProgArgName = "port"
  val LagomTimeoutProgArgName = "timeout"
}