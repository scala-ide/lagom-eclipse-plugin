package org.scalaide.lagom.cassandra

import scala.concurrent.duration.Deadline
import scala.concurrent.duration.FiniteDuration

import com.datastax.driver.core.Cluster
import com.lightbend.lagom.internal.cassandra.CassandraLauncher

import play.Logger

object LagomLauncher extends App {
  /**
   * Program attributes.
   * Keep in sync with [[org.scalaide.lagom.cassandra.LagomCassandraConfiguration]]
   */
  val LagomPortProgArgName = "port"
  val LagomTimeoutProgArgName = "timeout"
  val port = args.find(_.startsWith(LagomPortProgArgName)).map(_.drop(LagomPortProgArgName.length)).get.toInt
  val timeout = args.find(_.startsWith(LagomTimeoutProgArgName)).map(_.drop(LagomTimeoutProgArgName.length)).get.toInt

  private def waitForRunningCassandra(maxWaiting: FiniteDuration): Unit = {
    import scala.collection.JavaConverters._
    val hostname = "127.0.0.1"
    val contactPoint = Seq(new java.net.InetSocketAddress(hostname, port)).asJava
    val clusterBuilder = Cluster.builder.addContactPointsWithPorts(contactPoint)

    @annotation.tailrec
    def tryConnect(deadline: Deadline): Unit = {
      print(".") // each attempts prints a dot (informing the user of progress) 
      try {
        val session = clusterBuilder.build().connect()
        println() // we don't want to print the message on the same line of the dots... 
        session.closeAsync()
        session.getCluster.closeAsync()
        Logger.info("cassandra started")
      } catch {
        case _: Exception =>
          if (deadline.hasTimeLeft()) {
            // wait a bit before trying again
            Thread.sleep(500)
            tryConnect(deadline)
          } else {
            val msg = s"""Cassandra server is not yet started.\n
                           |The value assigned to
                           |$LagomTimeoutProgArgName
                           |is either too short, or this may indicate another 
                           |process is already running on port ${port}""".stripMargin
            println(msg) // we don't want to print the message on the same line of the dots...
          }
      }
    }
    tryConnect(maxWaiting.fromNow)
  }

  Logger.info("cassandra starting")
  new Thread {
    override def run = {
      import scala.concurrent.duration._
      waitForRunningCassandra(timeout seconds)
    }
  }.start()
  args.foreach(Logger.debug)
  CassandraLauncher.main(Array(port.toString))
}
