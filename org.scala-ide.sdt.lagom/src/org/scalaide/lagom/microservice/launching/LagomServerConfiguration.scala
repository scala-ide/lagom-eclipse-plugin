package org.scalaide.lagom.microservice.launching

object LagomServerConfiguration {
  val LagomServerPort = "LAGOM_SERVER_PORT"
  val LagomLocatorPort = "LAGOM_LOCATOR_PORT"
  val LagomCassandraPort = "LAGOM_CASSANDRA_PORT"
  val LagomWatchTimeout = "LAGOM_WATCH_TIMEOUT"
  val LagomServerPortDefault = 9099.toString
  val LagomLocatorPortDefault = 8000.toString
  val LagomCassandraPortDefault = 4000.toString
  val LagomWatchTimeoutDefault = 200.toString
  val LagomServiceRunnerClass = "org.scalaide.lagom.launching.LagomLauncher"
  val LagomApplicationLoaderPath = "play.application.loader"
}