package org.scalaide.lagom.kafka

import java.io.File

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.jdt.launching.IVMInstall
import org.eclipse.jdt.launching.IVMRunner
import org.eclipse.jdt.launching.JavaLaunchDelegate
import org.eclipse.jdt.launching.JavaRuntime
import org.eclipse.jdt.launching.VMRunnerConfiguration
import org.eclipse.m2e.core.MavenPlugin
import org.eclipse.m2e.core.embedder.ICallable
import org.scalaide.core.internal.launching.ClasspathGetterForLaunchDelegate
import org.scalaide.core.internal.launching.ProblemHandlersForLaunchDelegate
import org.scalaide.debug.internal.launching.StandardVMScalaDebugger
import org.scalaide.logging.HasLogger
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils
import org.eclipse.aether.resolution.DependencyRequest

trait LagomScalaDebuggerForLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
  override def getVMRunner(configuration: ILaunchConfiguration, mode: String): IVMRunner = {
    val vm = JavaRuntime.computeVMInstall(configuration)
    new LagomVMDebuggingRunner(vm)
  }
}

object Latch

object app {
  def apply() = {
    val locator = MavenRepositorySystemUtils.newServiceLocator()
    val system = newRepositorySystem(locator)
    val session = newSession(system)
    val central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build()
    val artifact = new DefaultArtifact("com.lightbend.lagom:lagom-kafka-server_2.11:1.3.5")
    import scala.collection.JavaConverters._
    val collectRequest = new CollectRequest(new Dependency(artifact, JavaScopes.COMPILE), List(central).asJava)
    val filter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE)
    val request = new DependencyRequest(collectRequest, filter)
    val result = system.resolveDependencies(session, request)
    result.getArtifactResults.forEach { artifact =>
      println(artifact.getArtifact.getFile)
    }
  }

  def newRepositorySystem(locator: DefaultServiceLocator): RepositorySystem = {
    locator.addService(classOf[RepositoryConnectorFactory], classOf[BasicRepositoryConnectorFactory])
    locator.addService(classOf[TransporterFactory], classOf[FileTransporterFactory])
    locator.addService(classOf[TransporterFactory], classOf[HttpTransporterFactory])
    locator.getService(classOf[RepositorySystem])
  }

  def newSession(system: RepositorySystem): RepositorySystemSession = {
    val session = MavenRepositorySystemUtils.newSession()
    val localRepo = new LocalRepository("target/local-repo")
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
    session
  }
}

class LagomVMDebuggingRunner(vm: IVMInstall) extends StandardVMScalaDebugger(vm) with HasLogger {
  private def collectDeps = {
    app()
  }

  private def addRunnerToClasspath(classpath: Array[String]): Array[String] = {
    val lagomBundle = Platform.getBundle("org.scala-ide.sdt.lagom")
    def findPath(lib: String) = {
      val libPath = new Path(s"runner-libs/$lib")
      val libBundleLocation = FileLocator.find(lagomBundle, libPath, null)
      val libFile = FileLocator.toFileURL(libBundleLocation)
      libFile.getPath
    }
    val paths = Seq("org.scala-ide.sdt.lagom.runner-1.0.0-SNAPSHOT.jar",
      "lagom-kafka-server_2.11-1.3.5.jar").map(findPath)
    classpath ++ paths
  }

  private def kafkaJVMOptions = Array("-Xms256m", "-Xmx1024m")

  private def asProject(name: String): IProject =
    ResourcesPlugin.getWorkspace.getRoot.getProject(name)

  private def targetDir(prjName: String) = {
    val prj = asProject(prjName)
    val prjLoc = prj.getLocation
    val javaPrj = JavaCore.create(prj)
    val outs = javaPrj.getResolvedClasspath(true).filter {
      _.getEntryKind == IClasspathEntry.CPE_SOURCE
    }.map { entry =>
      Option(entry.getOutputLocation)
    }.collect {
      case Some(out) => out
    }
    val ProjectRootSegment = 1
    val defaultOut = Option(javaPrj.getOutputLocation).map { out =>
      prjLoc.append(out.removeFirstSegments(ProjectRootSegment)).toFile.toURI.toURL
    }
    val out = outs.collect {
      case o =>
        prjLoc.append(o.removeFirstSegments(ProjectRootSegment)).toFile.toURI.toURL.getPath
    }.distinct.sorted.headOption
    defaultOut.orElse(out).get
  }

  import LagomKafkaConfiguration._
  override def run(config: VMRunnerConfiguration, launch: ILaunch, monitor: IProgressMonitor) = Latch.synchronized {
    val launchConfig = launch.getLaunchConfiguration
    val projectName = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
    val port = launchConfig.getAttribute(LagomPort, LagomPortDefault)
    val zookeeper = launchConfig.getAttribute(LagomZookeeperPort, LagomZookeeperPortDefault)
    collectDeps
    val / = File.separator
    val target = new File(targetDir(projectName) + / + "lagom-dynamic-projects" + / +
      "lagom-internal-meta-project-kafka" + / + "target").toURI.toURL.getPath
    val lagomConfig = new VMRunnerConfiguration(config.getClassToLaunch, addRunnerToClasspath(config.getClassPath) ++ config.getBootClassPath)
    lagomConfig.setBootClassPath(config.getBootClassPath)
    lagomConfig.setEnvironment(config.getEnvironment)
    lagomConfig.setProgramArguments(config.getProgramArguments ++
      Array(s"$LagomPortProgArgName$port",
        s"$LagomZookeeperPortProgArgName$zookeeper",
        s"$LagomTargetDirProgArgName$target"))
    lagomConfig.setResumeOnStartup(config.isResumeOnStartup)
    lagomConfig.setVMArguments(config.getVMArguments ++
      kafkaJVMOptions ++
      Array(s"-Dkafka.logs.dir=${target + /}log4j_output"))
    lagomConfig.setVMSpecificAttributesMap(config.getVMSpecificAttributesMap)
    lagomConfig.setWorkingDirectory(config.getWorkingDirectory)
    super.run(lagomConfig, launch, monitor)
  }
}

class LagomLaunchDelegate extends JavaLaunchDelegate with ClasspathGetterForLaunchDelegate
  with ProblemHandlersForLaunchDelegate with LagomScalaDebuggerForLaunchDelegate
