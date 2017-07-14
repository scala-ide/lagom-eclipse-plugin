package org.scalaide.lagom.kafka

import com.lightbend.lagom.internal.kafka.KafkaLauncher

object LagomLauncher extends App {
  /**
   * Program attributes.
   * Keep in sync with [[org.scalaide.lagom.kafka.LagomKafkaConfiguration]]
   */
  val LagomPortProgArgName = "port"
  val LagomZookeeperPortProgArgName = "zookeeper"
  val LagomTargetDirProgArgName = "targetdir"

  val port = args.find(_.startsWith(LagomPortProgArgName)).map(_.drop(LagomPortProgArgName.length)).get
  val zookeeper = args.find(_.startsWith(LagomZookeeperPortProgArgName)).map(_.drop(LagomZookeeperPortProgArgName.length)).get
  val targetDir = args.find(_.startsWith(LagomTargetDirProgArgName)).map(_.drop(LagomTargetDirProgArgName.length)).get

  try {
    KafkaLauncher.main((port :: zookeeper :: targetDir :: Nil).toArray)
  } catch {
    case e: Throwable => e.printStackTrace()
  }
}
