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

object LagomLauncher {
  val LagomLauncherClass = "org.scalaide.lagom.microservice.launching.LagomLauncher$"
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
                    Map("port" -> 2554)
                  )
                )
              )))

          try {
          import scala.concurrent.ExecutionContext.Implicits._
          Future {
          import scala.collection.JavaConverters._
          val serLoc = new ServiceLocatorServer()
          serLoc.start(3467, 0, Map.empty.asJava)
          }.map {_ =>

//          val cassConfig = {
//            val now = DateTimeFormatter.ofPattern("yyMMddHHmmssSSS").format(LocalDateTime.now())
//            val testName = s"ServiceTest_$now"
//            val cassandraPort = CassandraLauncher.randomPort
//            val cassandraDirectory = Files.createTempDirectory(testName).toFile
//            FileUtils.deleteRecursiveOnExit(cassandraDirectory)
//            CassandraLauncher.start(cassandraDirectory, "lagom-test-embedded-cassandra.yaml", clean = false, port = 0)
//            Configuration(TestUtil.persistenceConfig(testName, cassandraPort))
//          }

          val context = LagomApplicationContext(Context(Environment.simple(mode = Mode.Dev), None, new DefaultWebCommands, config /*++ cassConfig*/))
          appLoader.logger.info("################ starting... ###############")
          val lagomApplication = appLoader.loadDevMode(context)
          appLoader.logger.info("!!!!!!!!" + lagomApplication.serviceInfo.serviceName)
          //Play.start(lagomApplication.application)
          val serverConfig = ServerConfig(port = Some(9099), mode = lagomApplication.environment.mode, address = "localhost")
          val playServer = ServerProvider.defaultServerProvider.createServer(serverConfig, lagomApplication.application)
          }
//          lagomApplication match {
//            case requiresPort: RequiresLagomServicePort =>
//              requiresPort.provideLagomServicePort(playServer.httpPort.orElse(playServer.httpsPort).get)
//            case other => ()
//          }
//          Runtime.getRuntime.addShutdownHook {
//            new Thread { () =>
//              Try(Play.stop(lagomApplication.application))
//              Try(playServer.stop())
//              Try(CassandraLauncher.stop())
//              Try(serLoc.close())
//            }
//          }
          (1 to 10).foreach { _ => Thread.sleep(1000) }
          } catch {
            case e: Error => appLoader.logger.error("!!!!!!!!!!!" + e)
          }
        }
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
    }
  }
}
