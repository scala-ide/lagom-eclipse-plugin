package org.scalaide

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.text.MessageFormat

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages

import com.typesafe.config.ConfigFactory

package object lagom {
  object noLagomLoaderPath {
    import org.scalaide.lagom.microservice.LagomServerConfiguration._
    def apply(javaProject: IJavaProject): Boolean = try {
      val projectLocation = javaProject.getProject.getLocation
      val ProjectNameSegment = 1
      val configClassLoader = new URLClassLoader(javaProject.getResolvedClasspath(true).flatMap { cp =>
        Option(cp.getOutputLocation)
      }.distinct.map { icp =>
        projectLocation.append(icp.removeFirstSegments(ProjectNameSegment)).toFile.toURI.toURL
      } ++ Option(javaProject.getOutputLocation).map { o =>
        projectLocation.append(o.removeFirstSegments(ProjectNameSegment)).toFile.toURI.toURL
      }.toArray[URL])
      val projectConfig = ConfigFactory.load(configClassLoader)
      !projectConfig.hasPath(LagomApplicationLoaderPath)
    } catch {
      case allowToFailInRunner: Throwable => true
    }
  }

  object projectValidator {
    val LagomApplicationLoaderPath = "play.application.loader"

    private val noLagomLoaderPathInConfig: IProject => Boolean = (noLagomLoaderPath.apply _) compose (JavaCore.create _)

    def apply(name: String, setErrorMessage: String => Unit): PartialFunction[IProject, Boolean] = {
      PartialFunction.empty[IProject, Boolean].orElse[IProject, Boolean] {
        case project if !project.exists() =>
          setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_20, Array(name)))
          false
      }.orElse[IProject, Boolean] {
        case project if !project.isOpen =>
          setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_21, Array(name)))
          false
      }.orElse[IProject, Boolean] {
        case project if !project.hasNature(JavaCore.NATURE_ID) =>
          setErrorMessage(s"Project $name does not have Java nature.")
          false
      }.orElse[IProject, Boolean] {
        case project if noLagomLoaderPathInConfig(project) =>
          setErrorMessage(s"Project $name does not define $LagomApplicationLoaderPath path in configuration.")
          false
      }.orElse[IProject, Boolean] {
        case _ =>
          true
      }
    }
  }

  object mavenDeps {
    val MavenDelimeter = ":"
    val DefaultRemoteRepoId = "central"
    val DefaultRemoteRepoType = "default"
    val DefaultRemoteRepoUrl = "http://repo1.maven.org/maven2/"

    def apply(localRepoLocation: String)(groupId: String, artifactId: String, version: String): Seq[String] = {
      val locator = MavenRepositorySystemUtils.newServiceLocator()
      val system = newRepositorySystem(locator)
      val session = newSession(system, localRepoLocation)
      val artifact = new DefaultArtifact(Seq(groupId, artifactId, version).mkString(MavenDelimeter))
      val defaultRemote = new RemoteRepository.Builder(DefaultRemoteRepoId, DefaultRemoteRepoType, DefaultRemoteRepoUrl).build()
      import scala.collection.JavaConverters._
      val collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), List(defaultRemote).asJava)
      val filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE)
      val request = new DependencyRequest(collectRequest, filter)
      val result = system.resolveDependencies(session, request)
      result.getArtifactResults.asScala.map { artifact =>
        artifact.getArtifact.getFile
      }.map { file =>
        file.toURI.getPath
      }.toSeq
    }

    def defaultLocalRepoLocation(prjName: String): String = {
      val / = File.separator
      eclipseTools.asProject(prjName).getLocationURI.getPath + / + "target" + / + "local-repo"
    }

    private def newRepositorySystem(locator: DefaultServiceLocator): RepositorySystem = {
      locator.addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
      locator.addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
      locator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
      locator.getService(classOf[RepositorySystem])
    }

    private def newSession(system: RepositorySystem, localRepoLocation: String): RepositorySystemSession = {
      val session = MavenRepositorySystemUtils.newSession()
      val localRepo = new LocalRepository(localRepoLocation)
      session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
      session
    }
  }

  object addRunnerToClasspath {
    def apply(classpath: Array[String], fromRunnerLib: Seq[String] = Nil): Array[String] = {
      val lagomBundle = Platform.getBundle("org.scala-ide.sdt.lagom")
      def findPath(lib: String) = {
        val libPath = new Path(s"runner-libs/$lib")
        val libBundleLocation = FileLocator.find(lagomBundle, libPath, null)
        val libFile = FileLocator.toFileURL(libBundleLocation)
        libFile.getPath
      }
      val paths = Seq("org.scala-ide.sdt.lagom.runner-1.0.0-SNAPSHOT.jar") ++ fromRunnerLib map (findPath)
      classpath ++ paths
    }
  }

  object eclipseTools {
    def asProject(name: String): IProject =
      ResourcesPlugin.getWorkspace.getRoot.getProject(name)

    type ScalaVersion = String
    type LagomVersion = String
    val DefaultVersions = ("2.11", "1.3.5")
    def findLagomVersion(prj: IProject): (ScalaVersion, LagomVersion) =
      Option { if (prj.hasNature(JavaCore.NATURE_ID)) prj else null }.flatMap { prj =>
        val javaPrj = JavaCore.create(prj)
        import scala.collection.JavaConverters._
        javaPrj.getResolvedClasspath(true).collect {
          case entry if entry.getEntryKind == IClasspathEntry.CPE_LIBRARY =>
            entry.getPath.segments().last
        }.collectFirst {
          case lagomLib if isLagomLib(lagomLib) =>
            val versions = """(_(\d\.\d\d))?-(\d\.\d+\.\d+)\.jar""".r("ignore", "scalaVersion", "lagomVersion")
            versions.findAllMatchIn(lagomLib).toList.lastOption.map { m =>
              (m.group("scalaVersion"), m.group("lagomVersion"))
            }.collect {
              case (s, l) =>
                (Option(s).filter(_.nonEmpty).getOrElse(DefaultVersions._1),
                  Option(l).filter(_.nonEmpty).getOrElse(DefaultVersions._2))
            }
        }.flatten
      }.getOrElse(DefaultVersions)

    private def isLagomLib(potential: String) = potential.toLowerCase().contains("lagom")
  }
}
