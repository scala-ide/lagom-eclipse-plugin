package org.scalaide.lagom.kafka

object LagomKafkaConfiguration {
  // configuration keys
  val LagomPort = "LAGOM_KAFKA_PORT"
  val LagomZookeeperPort = "LAGOM_ZOOKEEPER_PORT"
  // defaults
  val LagomPortDefault = 9092.toString
  val LagomZookeeperPortDefault = 2181.toString
  val NotSet = ""
  // launch facilities
  val LagomKafkaRunnerClass = "org.scalaide.lagom.kafka.LagomLauncher"
  val LagomKafkaName = "lagom-kafka"
  // program attributes
  val LagomPortProgArgName = "port"
  val LagomZookeeperPortProgArgName = "zookeeper"
  val LagomTargetDirProgArgName = "targetdir"
  // -DKafka.cleanOnStart=<boolean>
  // -DKafka.propertiesFile=<string_file_location>
  // -Dkafka.logs.dir=<string_file_location>
}