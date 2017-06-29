package org.scalaide.lagom.launching

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
import akka.actor.ActorSystem
import play.core.ApplicationProvider
import akka.stream.ActorMaterializer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.io.File
import play.api.ApplicationLoader
import com.lightbend.lagom.dev.Reloader
import com.lightbend.lagom.dev.LagomConfig
import com.lightbend.lagom.dev.Reloader.CompileSuccess
import play.dev.filewatch.FileWatchService
import play.dev.filewatch.LoggerProxy

object LagomLauncher {
  object SbtLoggerProxy extends LoggerProxy {
    override def debug(message: => String): Unit = println(message)

    override def info(message: => String): Unit = println(message)

    override def warn(message: => String): Unit = println(message)

    override def error(message: => String): Unit = println(message)

    override def verbose(message: => String): Unit = println(message)

    override def success(message: => String): Unit = println(message)

    override def trace(t: => Throwable): Unit = println(t)
  }
  def main(args: Array[String]): Unit = {
    val workDir = args.find(_.startsWith("workdir")).map(_.substring("workdir".length))
    println(workDir.getOrElse("."))
    try {
      val reloadLock = LagomLauncher.getClass
      val devSettings =
        LagomConfig.actorSystemConfig("testMe-one") ++
          Option("http://127.0.0.1:8000").map(LagomConfig.ServiceLocatorUrl -> _).toMap ++
          Option(4000).fold(Map.empty[String, String]) { port =>
            LagomConfig.cassandraPort(port)
          }
      val watchService = FileWatchService.defaultWatchService(
        new File(workDir.getOrElse("."), "bin"),
        15000, SbtLoggerProxy)
      val service = Reloader.startDevMode(
        getClass.getClassLoader,
        Nil,
        () => CompileSuccess(Map.empty, Seq(new File(workDir.getOrElse("."), "bin"))),
        identity,
        Seq(new File(workDir.getOrElse("."), "src/main/scala")),
        watchService,
        new File(workDir.getOrElse(".")),
        devSettings.toSeq,
        9099,
        reloadLock) /*.startNoReload(getClass.getClassLoader,
        Nil,
        new File("."),
        devSettings.toSeq,
        9099)*/

      // Eagerly reload to start
      service.reload()
      service.addChangeListener(() => service.reload())

    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
    }
  }
}
