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

object LagomLauncher {
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
              case Nil => true
              case h +: Nil if h.isEmpty => true
              case _ => false
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
          val config = Configuration.from(Map("lagom.service-locator" -> Map("url" -> "http://localhost:3467"),
              ("akka" -> Map(
                "remote" -> Map(
                  "netty.tcp" ->
                    Map("port" -> 0)
                  ),
                "dev-mode" -> Map(
                  "actor-system" -> Map(
                    "name" -> "lagom-dev-mode"
                  ),
                "config" -> Map(
                  "log-dead-letter" -> "off",
                  "http.server.transparent-head-requests" -> false,
                  "akka.actor.provider" -> "akka.actor.LocalActorRefProvider"
                  )
                )
              ))))
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
          val context = LagomApplicationContext(Context(Environment.simple(mode = Mode.Dev), None, new DefaultWebCommands, config))
          appLoader.logger.info("################ starting... ###############")
          val lagomApplication = appLoader.loadDevMode(context)
          appLoader.logger.info("!!!!!!!!" + lagomApplication.serviceInfo.serviceName)
          val serverConfig = ServerConfig(port = Some(9099), mode = lagomApplication.environment.mode, address = "localhost")
          val devModeAkkaConfig = lagomApplication.configuration.underlying.getConfig("akka.dev-mode.config")
          val actorSystemName = lagomApplication.configuration.underlying.getString("akka.dev-mode.actor-system.name")
          val actorSystem = ActorSystem(actorSystemName, devModeAkkaConfig)
          val playServer = ServerProvider.defaultServerProvider.createServer(ServerProvider.Context(serverConfig, ApplicationProvider(lagomApplication.application),
              actorSystem, lagomApplication.materializer, () => Future.successful(())))
          lagomApplication match {
            case requiresPort: RequiresLagomServicePort =>
              requiresPort.provideLagomServicePort(playServer.httpPort.orElse(playServer.httpsPort).get)
            case other => ()
          }
          Play.start(lagomApplication.application)
          Runtime.getRuntime.addShutdownHook {
            new Thread { () =>
              Try(Play.stop(lagomApplication.application))
              Try(playServer.stop())
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
