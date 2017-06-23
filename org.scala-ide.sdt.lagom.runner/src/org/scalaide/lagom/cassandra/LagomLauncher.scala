package org.scalaide.lagom.cassandra

import scala.concurrent.duration.Deadline
import scala.concurrent.duration.FiniteDuration

import com.datastax.driver.core.Cluster
import com.lightbend.lagom.internal.cassandra.CassandraLauncher

object LagomLauncher extends App {
  val port :: _ = args.toList
  private def waitForRunningCassandra(maxWaiting: FiniteDuration): Unit = {
    import scala.collection.JavaConverters._
    val hostname = "127.0.0.1"
    val contactPoint = Seq(new java.net.InetSocketAddress(hostname, port.toInt)).asJava
    val clusterBuilder = Cluster.builder.addContactPointsWithPorts(contactPoint)

    @annotation.tailrec
    def tryConnect(deadline: Deadline): Unit = {
      print(".") // each attempts prints a dot (informing the user of progress) 
      try {
        val session = clusterBuilder.build().connect()
        println() // we don't want to print the message on the same line of the dots... 
        session.closeAsync()
        session.getCluster.closeAsync()
        println("cassandra started")
      } catch {
        case _: Exception =>
          if (deadline.hasTimeLeft()) {
            // wait a bit before trying again
            Thread.sleep(500)
            tryConnect(deadline)
          } else {
            val msg = s"""Cassandra server is not yet started.\n
                           |The value assigned to
                           |`lagomCassandraMaxBootWaitingTime`
                           |is either too short, or this may indicate another 
                           |process is already running on port ${port}""".stripMargin
            println(msg) // we don't want to print the message on the same line of the dots...
          }
      }
    }
    tryConnect(maxWaiting.fromNow)
  }
  new Thread { override def run = {
    import scala.concurrent.duration._
    waitForRunningCassandra(200 seconds)
  }
  }.start()
  println("!!!! CASSANDRA !!!!!")
  CassandraLauncher.main(args)
  println("OUT")
}
