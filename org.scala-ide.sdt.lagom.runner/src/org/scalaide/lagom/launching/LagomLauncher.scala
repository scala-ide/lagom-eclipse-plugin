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

object LagomLauncher {
  val LagomLauncherClass = "org.scalaide.lagom.launching.LagomLauncher$"
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
          val config = Configuration.empty
          val context = LagomApplicationContext(Context(Environment.simple(mode = Mode.Dev), None, new DefaultWebCommands, config))
          appLoader.loadDevMode(context)
        }.map { lagomApplication =>
          Play.start(lagomApplication.application)
          val serverConfig = ServerConfig(port = Some(0), mode = lagomApplication.environment.mode)
          val playServer = ServerProvider.defaultServerProvider.createServer(serverConfig, lagomApplication.application)
          lagomApplication match {
            case requiresPort: RequiresLagomServicePort =>
              requiresPort.provideLagomServicePort(playServer.httpPort.orElse(playServer.httpsPort).get)
            case other => ()
          }
          Runtime.getRuntime.addShutdownHook {
            new Thread { () =>
              Try(Play.stop(lagomApplication.application))
              Try(playServer.stop())
              Try(CassandraLauncher.stop())
            }
          }
          (1 to 100).foreach { _ => Thread.sleep(100) }
              Try(Play.stop(lagomApplication.application))
              Try(playServer.stop())
              Try(CassandraLauncher.stop())
        }
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
    }
  }
}
