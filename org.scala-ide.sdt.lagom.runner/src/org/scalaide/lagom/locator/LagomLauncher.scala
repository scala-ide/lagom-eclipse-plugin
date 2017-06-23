package org.scalaide.lagom.locator

import scala.util.Try

import com.lightbend.lagom.scaladsl.server.LagomApplicationContext
import com.lightbend.lagom.scaladsl.server.LagomApplicationLoader
import com.lightbend.lagom.scaladsl.server.RequiresLagomServicePort

import akka.persistence.cassandra.testkit.CassandraLauncher
import play.api.ApplicationLoader.Context
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import play.api.Play
import play.core.DefaultWebCommands
import play.core.server.ServerConfig
import play.core.server.ServerProvider
import java.time.format.DateTimeFormatter
import java.nio.file.Files
import org.apache.cassandra.io.util.FileUtils
import com.lightbend.lagom.scaladsl.persistence.cassandra.testkit.TestUtil
import java.time.LocalDateTime
import com.lightbend.lagom.discovery.ServiceLocatorServer
import scala.concurrent.Future

object LagomLauncher {
  def main(args: Array[String]): Unit = {
    try {
      import scala.collection.JavaConverters._
      val serLoc = new ServiceLocatorServer()
      serLoc.start(3467, 9000, Map( "cas_native" -> "http://127.0.0.1:2345/cas_native").asJava)
      Runtime.getRuntime.addShutdownHook {
        new Thread { () =>
          Try(serLoc.close())
        }
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
    }
  }
}
