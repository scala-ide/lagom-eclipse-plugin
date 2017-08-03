package org.scalaide.lagom.microservice

object LagomServerConfiguration {
  // configuration keys
  val LagomServerPort = "LAGOM_SERVER_PORT"
  val LagomLocatorPort = "LAGOM_LOCATOR_PORT"
  val LagomCassandraPort = "LAGOM_CASSANDRA_PORT"
  val LagomWatchTimeout = "LAGOM_WATCH_TIMEOUT"
  // defaults
  val LagomServerPortDefault = 9099.toString
  val LagomLocatorPortDefault = 8000.toString
  val LagomCassandraPortDefault = 4000.toString
  val LagomWatchTimeoutDefault = 200.toString
  val NotSet = ""
  // launch facilities
  val LagomServiceRunnerClass = "org.scalaide.lagom.microservice.LagomLauncher"
  val LagomApplicationLoaderPath = "play.application.loader"
  // program attributes
  val LagomSourceDirsProgArgName = "srcdirs"
  val LagomOutputDirsProgArgName = "outdirs"
  val LagomProjectProgArgName = "proj"
  val LagomServicePathProgArgName = "servicepath"
  val LagomServerPortProgArgName = "servport"
  val LagomLocatorPortProgArgName = "locport"
  val LagomCassandraPortProgArgName = "cassport"
  val LagomWatchTimeoutProgArgName = "wtime"
  val LagomWorkDirProgArgName = "workdir"
}
