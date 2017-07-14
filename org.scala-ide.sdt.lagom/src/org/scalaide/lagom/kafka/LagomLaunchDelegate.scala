package org.scalaide.lagom.kafka

import java.io.File

import org.eclipse.aether.util.artifact.JavaScopes
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

import com.jcabi.aether.Aether

trait LagomScalaDebuggerForLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
  override def getVMRunner(configuration: ILaunchConfiguration, mode: String): IVMRunner = {
    val vm = JavaRuntime.computeVMInstall(configuration)
    new LagomVMDebuggingRunner(vm)
  }
}

object Latch

class LagomVMDebuggingRunner(vm: IVMInstall) extends StandardVMScalaDebugger(vm) with HasLogger {
  private def collectDeps = {
    val mvn = MavenPlugin.getMaven
    val call2: ICallable[Seq[String]] = (context, monitor) => Latch.synchronized {
      val session = context.getSession
      val repo = session.getLocalRepository.getBasedir
      import org.eclipse.aether.artifact.{ DefaultArtifact => _ }
      import org.eclipse.aether.repository.{ RemoteRepository => _ }
      import org.sonatype.aether.util.artifact.DefaultArtifact
      import org.sonatype.aether.repository.RemoteRepository
      import scala.collection.JavaConverters._
      val rr = new RemoteRepository("maven-central",
        "default",
        "http://repo1.maven.org/maven2/")
      val qq = new RemoteRepository("local",
        "default",
        session.getLocalRepository.getUrl)
      
      val deps = new Aether(Seq(rr, qq).asJava, new File(repo)).resolve(
        new DefaultArtifact("com.lightbend.lagom", "lagom-kafka-server_2.11",
          "", "jar", "1.3.5"),
        JavaScopes.RUNTIME)
      import scala.collection.JavaConverters._
      deps.asScala.map(_.getArtifactId)
    }
//    val call: ICallable[Seq[Artifact]] = (context, monitor) => {
//      val dependency = {
//        val artifact = new DefaultArtifact("com.lightbend.lagom", "lagom-kafka-server_2.11",
//          "jar", "1.3.5")
//        new Dependency(artifact, "runtime")
//      }
//
//      /**
//       * Resolve the classpath for the given dependency.
//       */
//      val session = context.getSession
//      def resolveDependency(dependency: Dependency, additionalDependencies: Seq[Dependency] = Nil): Seq[Artifact] = {
//        val collect = new CollectRequest()
//        collect.setRoot(dependency)
//        import scala.collection.JavaConverters._
//        collect.setRepositories(mvn.getArtifactRepositories.asScala.toList.map { repo =>
//          new Builder("maven-central",
//            "default",
//            "http://repo1.maven.org/maven2/").build
//        }.asJava)
//        additionalDependencies.foreach(collect.addDependency)
//
//        toDependencies(resolveDependencies(collect)).map(_.getArtifact)
//      }
//      def toDependencies(depResult: DependencyResult): Seq[Dependency] = {
//        import scala.collection.JavaConverters._
//        depResult.getArtifactResults.asScala.map(_.getRequest.getDependencyNode.getDependency)
//      }
//      def resolveDependencies(collect: CollectRequest): DependencyResult = {
//        val depRequest = new DependencyRequest(collect, null)
//
//        import scala.collection.JavaConverters._
//        // Replace the workspace reader with one that will resolve projects that haven't been compiled yet
//        //val repositorySession = new DefaultRepositorySystemSession(session.getRepositorySession)
//        //repositorySession.setWorkspaceReader(new UnbuiltWorkspaceReader(repositorySession.getWorkspaceReader, session))
//        val polProv = new DefaultChecksumPolicyProvider
//
//        val polAnal = new DefaultUpdatePolicyAnalyzer
//
//        val repoMngr = new DefaultRemoteRepositoryManager
//        repoMngr.setChecksumPolicyProvider(polProv)
//        repoMngr.setUpdatePolicyAnalyzer(polAnal)
//
//        val policeMngr = new DefaultUpdatePolicyAnalyzer
//
//        val fileProc = new DefaultFileProcessor
//
//        val container = new DefaultPlexusContainer
//
//        val wagProv = new PlexusWagonProvider(container)
//        //wagProv.
//
//        val wagConf = new PlexusWagonConfigurator
//
//        val transF = new WagonTransporterFactory
//        transF.setWagonProvider(wagProv)
//        transF.setWagonConfigurator(wagConf)
//
//        val transport = new DefaultTransporterProvider
//        transport.setTransporterFactories(Seq(transF.asInstanceOf[TransporterFactory]).asJava)
//
//        val mavenLF = new Maven2RepositoryLayoutFactory
//
//        val layout = new DefaultRepositoryLayoutProvider
//        layout.setRepositoryLayoutFactories(Seq(mavenLF.asInstanceOf[RepositoryLayoutFactory]).asJava)
//
//        val bConFac = new BasicRepositoryConnectorFactory
//        bConFac.setChecksumPolicyProvider(polProv)
//        bConFac.setFileProcessor(fileProc)
//        bConFac.setTransporterProvider(transport)
//        bConFac.setRepositoryLayoutProvider(layout)
//
//        val connProv = new DefaultRepositoryConnectorProvider
//        connProv.setRepositoryConnectorFactories(Seq(bConFac.asInstanceOf[RepositoryConnectorFactory]).asJava)
//
//        val syncContextFactory = new DefaultSyncContextFactory
//
//        val eventDisp = new DefaultRepositoryEventDispatcher
//        eventDisp.setRepositoryListeners(Seq(session.getRepositorySession.getRepositoryListener).asJava)
//
//        val upMngr = new DefaultUpdateCheckManager
//        upMngr.setUpdatePolicyAnalyzer(policeMngr)
//
//        val versResolv = new DefaultVersionResolver
//        versResolv.setRepositoryEventDispatcher(eventDisp)
//        versResolv.setSyncContextFactory(syncContextFactory)
//
//        val artResolver = new DefaultArtifactResolver
//        artResolver.setRepositoryEventDispatcher(eventDisp)
//        artResolver.setSyncContextFactory(syncContextFactory)
//        artResolver.setVersionResolver(versResolv)
//        artResolver.setRepositoryConnectorProvider(connProv)
//        artResolver.setUpdateCheckManager(upMngr)
//        artResolver.setRemoteRepositoryManager(repoMngr)
//
//        val reader = new DefaultArtifactDescriptorReader
//        reader.setVersionResolver(versResolv)
//        reader.setArtifactResolver(artResolver)
//        reader.setModelBuilder(new DefaultModelBuilder)
//
//        val collector = new DefaultDependencyCollector
//        collector.setArtifactDescriptorReader(reader)
//        collector.setRemoteRepositoryManager(repoMngr)
//        collector.setVersionRangeResolver(new DefaultVersionRangeResolver)
//
//        val repoSystem = new DefaultRepositorySystem
//        repoSystem.setDependencyCollector(collector)
//        val collectResult = repoSystem.collectDependencies(context.getRepositorySession, collect)
//
//        val node = collectResult.getRoot
//        depRequest.setRoot(node)
//
//        repoSystem.resolveDependencies(context.getRepositorySession, depRequest)
//      }
//      def resolveArtifact(artifact: Artifact): Seq[Artifact] = {
//        resolveDependency(new Dependency(artifact, "runtime"))
//      }
//      val cp = resolveArtifact(dependency.getArtifact)
//      cp
//    }
    val cps = mvn.execute(true, true, call2, new NullProgressMonitor)
    println(cps)
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
