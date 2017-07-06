package org.scalaide.lagom.launching

import java.io.File

import com.lightbend.lagom.dev.LagomConfig
import com.lightbend.lagom.dev.Reloader
import com.lightbend.lagom.dev.Reloader.CompileSuccess

import play.dev.filewatch.FileWatchService
import play.dev.filewatch.LoggerProxy

object LagomLauncher {
  /**
   * Program attributes.
   * Keep in sync with [[org.scalaide.lagom.microservice.launching.LagomServerConfiguration]]
   */
  val LagomSourceDirsProgArgName = "srcdirs"
  val LagomOutputDirsProgArgName = "outdirs"
  val LagomProjectProgArgName = "proj"
  val LagomServicePathProgArgName = "servicepath"
  val LagomServerPortProgArgName = "servport"
  val LagomLocatorPortProgArgName = "locport"
  val LagomCassandraPortProgArgName = "cassport"
  val LagomWatchTimeoutProgArgName = "wtime"
  val LagomWorkDirProgArgName = "workdir"

  val hostname = "http://127.0.0.1"

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
    val srcs = args.find(_.startsWith(LagomSourceDirsProgArgName)).map(_.drop(LagomSourceDirsProgArgName.length)).get
    val outs = args.find(_.startsWith(LagomOutputDirsProgArgName)).map(_.drop(LagomOutputDirsProgArgName.length)).get
    val serverPort = args.find(_.startsWith(LagomServerPortProgArgName)).map(_.drop(LagomServerPortProgArgName.length)).get.toInt
    val servicePath = args.find(_.startsWith(LagomServicePathProgArgName)).map(_.drop(LagomServicePathProgArgName.length)).get
    val watchTimeout = args.find(_.startsWith(LagomWatchTimeoutProgArgName)).map(_.drop(LagomWatchTimeoutProgArgName.length)).get.toInt
    val projectName = args.find(_.startsWith(LagomProjectProgArgName)).map(_.drop(LagomProjectProgArgName.length)).get
    val workingDir = args.find(_.startsWith(LagomWorkDirProgArgName)).map { w =>
      new File(w.drop(LagomWorkDirProgArgName.length))
    }.get
    val locatorPort = args.find(_.startsWith(LagomLocatorPortProgArgName))
    val cassandraPort = args.find(_.startsWith(LagomCassandraPortProgArgName)).map(_.toInt)
    println(servicePath)
    try {
      val reloadLock = LagomLauncher
      val devSettings =
        LagomConfig.actorSystemConfig(projectName) ++
        locatorPort.map(hostname + ":" + _).map(LagomConfig.ServiceLocatorUrl -> _).toMap ++
        cassandraPort.fold(Map.empty[String, String])(LagomConfig.cassandraPort)
      val outputDirForWatchServiceWhichCanBeAny = new File(outs.split(File.pathSeparator).head)
      val watchService = FileWatchService.defaultWatchService(
        outputDirForWatchServiceWhichCanBeAny,
        watchTimeout,
        SbtLoggerProxy
      )
      val serviceClassPath = servicePath.split(File.pathSeparator).map(new File(_)).toSeq
      val sourcesToWatch = srcs.split(File.pathSeparator).map(new File(_)).toSeq
      val service = Reloader.startDevMode(
        getClass.getClassLoader,
        Nil,
        () => { CompileSuccess(Map.empty, serviceClassPath) },
        identity,
        sourcesToWatch,
        watchService,
        workingDir,
        devSettings.toSeq,
        serverPort,
        reloadLock
      )

      // Eagerly reload to start
      service.reload()
      service.addChangeListener { () =>
        println("in changed")
        service.reload()
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}
