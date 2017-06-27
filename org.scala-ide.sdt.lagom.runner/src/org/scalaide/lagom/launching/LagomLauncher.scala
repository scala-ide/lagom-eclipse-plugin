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

object LagomLauncher1 {
  val LagomClassSwitch = "lagomclass"

  def main(args: Array[String]): Unit = {
    try {
      args.collectFirst {
        case n if n.startsWith(LagomClassSwitch) =>
          n.replaceFirst(LagomClassSwitch, "")
      }.foreach { className =>
        import scala.reflect.runtime.{ universe => ru }
        val mirror = ru.runtimeMirror(getClass.getClassLoader)
        val classSymbol = mirror.classSymbol(Class.forName(className))
        val isDefaultConstructor: PartialFunction[ru.Symbol, Boolean] = {
          case c if c.isConstructor =>
            c.asMethod.paramLists match {
              case Nil                   => true
              case h +: Nil if h.isEmpty => true
              case _                     => false
            }
          case _ => false
        }
        classSymbol.typeSignature.members.collectFirst {
          case c if isDefaultConstructor(c) =>
            c.asMethod
        }.map { constructor =>
          val classToInstantiate = mirror.reflectClass(classSymbol)
          val toInvoke = classToInstantiate.reflectConstructor(constructor)
          val a = classSymbol.toType
          toInvoke().asInstanceOf[LagomApplicationLoader]
        }.map { appLoader =>
          val config = Configuration.from(Map("lagom" -> Map("service-locator" -> Map("url" -> "http://localhost:8000"),
            "akka" -> Map(
              "dev-mode" -> Map(
                "actor-system" -> Map(
                  "name" -> "lagom-dev-mode"),
                "config" -> Map(
                  "log-dead-letter" -> "off",
                  "http.server.transparent-head-requests" -> false,
                  "akka.actor.provider" -> "akka.actor.LocalActorRefProvider")))),
            "akka" -> Map(
              "remote" -> Map(
                "netty.tcp" ->
                  Map("port" -> 0))))) ++ Configuration("lagom.defaults.cluster.join-self" -> "on")
          /*
dev-mode {
      actor-system {
        name = "lagom-dev-mode"
      }

      # The dev mode actor system. Lagom typically uses the application actor system, however, in dev mode, an actor
      # system is needed that outlives the application actor system, since the HTTP server will need to use this, and it
      # lives through many application (and therefore actor system) restarts.
      config {
        # Turn off dead letters until Akka HTTP server is stable
        log-dead-letters = off

        # Disable Akka-HTTP's transparent HEAD handling. so that play's HEAD handling can take action
        http.server.transparent-head-requests = false

        akka.actor.provider = akka.actor.LocalActorRefProvider
      }
    }
 */
          val env = Environment(new File("."), getClass.getClassLoader, Mode.Dev)
          val configuration = Configuration.load(env)

          val contextA = Context(env, None, new DefaultWebCommands, configuration ++ config)
          val context = LagomApplicationContext(contextA)
          appLoader.logger.info("################ starting... ###############")
          val lagomApplication = appLoader.loadDevMode(context)
          appLoader.logger.info("!!!!!!!!" + lagomApplication.serviceInfo.serviceName)
          import scala.concurrent.ExecutionContext.Implicits._
          Await.ready(
            Future {
              Play.start(lagomApplication.application)
            }, Duration.Inf)
          //val serverConfig = ServerConfig(port = Some(9099), mode = lagomApplication.environment.mode, address = "localhost")
          val serverConfig = ServerConfig(
            rootDir = context.playContext.environment.rootPath,
            port = Option(0),
            sslPort = None,
            address = "localhost",
            mode = lagomApplication.environment.mode,
            properties = System.getProperties,
            configuration = Configuration.load(lagomApplication.environment) ++ config)
          // We *must* use a different Akka configuration in dev mode, since loading two actor systems from the same
          // config will lead to resource conflicts, for example, if the actor system is configured to open a remote port,
          // then both the dev mode and the application actor system will attempt to open that remote port, and one of
          // them will fail.
          val devModeAkkaConfig = serverConfig.configuration.underlying.getConfig("lagom.akka.dev-mode.config")
          val actorSystemName = serverConfig.configuration.underlying.getString("lagom.akka.dev-mode.actor-system.name")
          val actorSystem = ActorSystem(actorSystemName, devModeAkkaConfig)
          val serverContext = ServerProvider.Context(serverConfig, ApplicationProvider(lagomApplication.application), actorSystem, ActorMaterializer()(actorSystem),
            () => actorSystem.terminate())
          val serverProvider = ServerProvider.fromConfiguration(context.playContext.environment.classLoader, serverConfig.configuration)
          val playServer = serverProvider.createServer(serverContext)
          Await.ready(
            Future {
              Play.start(lagomApplication.application)
            }, Duration.Inf)

          //          lagomApplication match {
          //            case requiresPort: RequiresLagomServicePort =>
          //              requiresPort.provideLagomServicePort(playServer.httpPort.orElse(playServer.httpsPort).get)
          //            case other => ()
          //          }
          Runtime.getRuntime.addShutdownHook {
            new Thread {
              override def run {
                Try(Play.stop(lagomApplication.application))
                Try(playServer.stop())
              }
            }
          }
        }
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
    }
  }
}

object LagomLauncher {
  def main(args: Array[String]): Unit = {
    try {
      val reloadLock = LagomLauncher.getClass
      val devSettings =
        LagomConfig.actorSystemConfig("testMe-one") ++
          Option("http://127.0.0.1:8000").map(LagomConfig.ServiceLocatorUrl -> _).toMap ++
          Option(4000).fold(Map.empty[String, String]) { port =>
            LagomConfig.cassandraPort(port)
          }
      val service = Reloader.startNoReload(getClass.getClassLoader,
        Nil,
        new File("."),
        devSettings.toSeq,
        9099)

      // Eagerly reload to start
      service.reload()

    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
    }
  }
}
