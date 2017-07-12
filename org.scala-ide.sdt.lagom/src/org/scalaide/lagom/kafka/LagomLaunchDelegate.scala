package org.scalaide.lagom.kafka

import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate
import org.eclipse.jdt.launching.IVMInstall
import org.eclipse.jdt.launching.IVMRunner
import org.eclipse.jdt.launching.JavaLaunchDelegate
import org.eclipse.jdt.launching.JavaRuntime
import org.eclipse.jdt.launching.VMRunnerConfiguration
import org.scalaide.core.internal.launching.ClasspathGetterForLaunchDelegate
import org.scalaide.core.internal.launching.ProblemHandlersForLaunchDelegate
import org.scalaide.debug.internal.launching.StandardVMScalaDebugger
import org.scalaide.logging.HasLogger
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.debug.ui.IJavaDebugUIConstants
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.IClasspathEntry
import java.io.File

trait LagomScalaDebuggerForLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
  override def getVMRunner(configuration: ILaunchConfiguration, mode: String): IVMRunner = {
    val vm = JavaRuntime.computeVMInstall(configuration)
    new LagomVMDebuggingRunner(vm)
  }
}

class LagomVMDebuggingRunner(vm: IVMInstall) extends StandardVMScalaDebugger(vm) with HasLogger {
  private def addRunnerToClasspath(classpath: Array[String]): Array[String] = {
    val lagomBundle = Platform.getBundle("org.scala-ide.sdt.lagom")
    def findPath(lib: String) = {
      val libPath = new Path(s"runner-libs/$lib")
      val libBundleLocation = FileLocator.find(lagomBundle, libPath, null)
      val libFile = FileLocator.toFileURL(libBundleLocation)
      libFile.getPath
    }
    val paths = Seq("org.scala-ide.sdt.lagom.runner-1.0.0-SNAPSHOT.jar",
        "lagom-kafka-server_2.11-1.3.5.jar"
    ).map(findPath)
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
    val targetDir = defaultOut.orElse(out).get
  }

  import LagomKafkaConfiguration._
  override def run(config: VMRunnerConfiguration, launch: ILaunch, monitor: IProgressMonitor) = {
    val launchConfig = launch.getLaunchConfiguration
    val projectName = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
    val port = launchConfig.getAttribute(LagomPort, LagomPortDefault)
    val zookeeper = launchConfig.getAttribute(LagomZookeeperPort, LagomZookeeperPortDefault)
    val / = File.separator
    val target = new File(targetDir(projectName) + / + "lagom-dynamic-projects" + / +
      "lagom-internal-meta-project-kafka" + / + "target").toURI.toURL.getPath
    val lagomConfig = new VMRunnerConfiguration(config.getClassToLaunch, addRunnerToClasspath(config.getClassPath) ++ config.getBootClassPath)
    lagomConfig.setBootClassPath(config.getBootClassPath)
    lagomConfig.setEnvironment(config.getEnvironment)
    lagomConfig.setProgramArguments(config.getProgramArguments ++
      Array(s"$LagomPortProgArgName$port",
        s"$LagomZookeeperPortProgArgName$zookeeper",
        s"$LagomTargetDirProgArgName$target")
    )
    lagomConfig.setResumeOnStartup(config.isResumeOnStartup)
    lagomConfig.setVMArguments(config.getVMArguments ++
      kafkaJVMOptions ++
      Array(s"-Dkafka.logs.dir=${target + /}log4j_output")
    )
    lagomConfig.setVMSpecificAttributesMap(config.getVMSpecificAttributesMap)
    lagomConfig.setWorkingDirectory(config.getWorkingDirectory)
    super.run(lagomConfig, launch, monitor)
  }
}

class LagomLaunchDelegate extends JavaLaunchDelegate with ClasspathGetterForLaunchDelegate
  with ProblemHandlersForLaunchDelegate with LagomScalaDebuggerForLaunchDelegate