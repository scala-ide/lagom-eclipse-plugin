package org.scalaide.lagom.microservice

import java.io.File

import com.lightbend.lagom.dev.LagomConfig
import com.lightbend.lagom.dev.Reloader
import com.lightbend.lagom.dev.Reloader.CompileSuccess

import play.Logger
import play.dev.filewatch.FileWatchService
import play.dev.filewatch.LoggerProxy

object LagomLauncher {
  /**
   * Program attributes.
   * Keep in sync with [[org.scalaide.lagom.microservice.LagomServerConfiguration]]
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

  object LagomLauncherLogger extends LoggerProxy {
    override def debug(message: => String): Unit = Logger.debug(message)
    override def info(message: => String): Unit = Logger.info(message)
    override def warn(message: => String): Unit = Logger.warn(message)
    override def error(message: => String): Unit = Logger.error(message)
    override def verbose(message: => String): Unit = Logger.info(message)
    override def success(message: => String): Unit = Logger.info(message)
    override def trace(t: => Throwable): Unit = Logger.trace("", t)
  }

  /**
   * Mimics recompilation s with just timeout. Actual implementation should rather wait for event from
   * compiler about done job but there is not possibility to get such info from eclipse in this place.
   */
  private def mimicRecompilationWithTimeout(serviceClassPath: Seq[File], watchTimeout: Int) = () => {
    Thread.sleep(watchTimeout)
    CompileSuccess(Map.empty, serviceClassPath)
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
    val locatorPort = args.find(_.startsWith(LagomLocatorPortProgArgName)).map(_.drop(LagomLocatorPortProgArgName.length))
    val cassandraPort = args.find(_.startsWith(LagomCassandraPortProgArgName)).map(_.drop(LagomCassandraPortProgArgName.length).toInt)
    val log = Logger.info(_:String)
    log(s"Starting service $projectName")
    try {
      val reloadLock = LagomLauncher
      val devSettings =
        LagomConfig.actorSystemConfig(projectName) ++
        locatorPort.map(hostname + ":" + _).map(LagomConfig.ServiceLocatorUrl -> _).toMap ++
        cassandraPort.fold(Map.empty[String, String])(LagomConfig.cassandraPort)
      val outputDirForWatchServiceWhichCanBeAny = new File(outs.split(File.pathSeparator).head)
      val watchService = FileWatchService.defaultWatchService(
        outputDirForWatchServiceWhichCanBeAny,
        200, // rewritten from maven plugin
        LagomLauncherLogger
      )
      val serviceClassPath = servicePath.split(File.pathSeparator).map(new File(_)).toSeq
      val sourcesToWatch = srcs.split(File.pathSeparator).map(new File(_)).toSeq
      val recompile = mimicRecompilationWithTimeout(serviceClassPath, watchTimeout)
      val service = Reloader.startDevMode(
        getClass.getClassLoader,
        Nil,
        recompile,
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
      log(s"Service $projectName started")
      service.addChangeListener { () =>
        log(s"Reloading service $projectName. Check for RELOAD message or increase watch timeout.")
        service.reload()
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }
}
