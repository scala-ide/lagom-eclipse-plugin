package org.scalaide.lagom.microservice.launching

import java.io.File

import org.eclipse.core.runtime.IProgressMonitor
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
import org.scalaide.core.internal.launching.ClasspathGetterForLaunchDelegate
import org.scalaide.core.internal.launching.ProblemHandlersForLaunchDelegate
import org.scalaide.debug.internal.launching.StandardVMScalaDebugger
import org.scalaide.logging.HasLogger

trait LagomScalaDebuggerForLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
  override def getVMRunner(configuration: ILaunchConfiguration, mode: String): IVMRunner = {
    val vm = JavaRuntime.computeVMInstall(configuration)
    new LagomVMDebuggingRunner(vm)
  }
}

class LagomVMDebuggingRunner(vm: IVMInstall) extends StandardVMScalaDebugger(vm) with HasLogger {
  import LagomServerConfiguration._
  private def optionalProgArgs(locatorPort: String, cassandraPort: String): Array[String] = {
    val collect = scala.collection.mutable.ArrayBuffer.empty[String]
    if (locatorPort.nonEmpty) collect += s"$LagomLocatorPortProgArgName$locatorPort"
    if (cassandraPort.nonEmpty) collect += s"$LagomCassandraPortProgArgName$cassandraPort"
    collect.toArray
  }

  import org.scalaide.lagom.eclipseTools._
  private def srcsAndOutsProgArgs(project: String): Array[String] = {
    val collect = scala.collection.mutable.ArrayBuffer.empty[String]
    val prj = asProject(project)
    val prjLoc = prj.getLocation
    val javaPrj = JavaCore.create(prj)
    val (srcs, outs) = (javaPrj.getResolvedClasspath(true).filter {
      _.getEntryKind == IClasspathEntry.CPE_SOURCE
    }.map { entry =>
      Option(entry.getPath) -> Option(entry.getOutputLocation)
    } ++ Array(None -> Option(javaPrj.getOutputLocation))).unzip
    val ProjectRootSegment = 1
    val srcLocations = srcs.collect {
      case Some(s) =>
        prjLoc.append(s.removeFirstSegments(ProjectRootSegment)).toFile.toURI.toURL.getPath
    }.distinct.mkString(File.pathSeparator)
    val outLocations = outs.collect {
      case Some(o) =>
        prjLoc.append(o.removeFirstSegments(ProjectRootSegment)).toFile.toURI.toURL.getPath
    }.distinct.mkString(File.pathSeparator)
    collect += s"$LagomSourceDirsProgArgName$srcLocations"
    collect += s"$LagomOutputDirsProgArgName$outLocations"
    collect.toArray
  }

  override def run(config: VMRunnerConfiguration, launch: ILaunch, monitor: IProgressMonitor) = {
    val launchConfig = launch.getLaunchConfiguration
    val projectName = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
    val serverPort = launchConfig.getAttribute(LagomServerPort, LagomServerPortDefault)
    val watchTimeout = launchConfig.getAttribute(LagomWatchTimeout, LagomWatchTimeoutDefault)
    val locatorPort = launchConfig.getAttribute(LagomLocatorPort, NotSet)
    val cassandraPort = launchConfig.getAttribute(LagomCassandraPort, NotSet)
    val (servicePath, dependenciesPath) = config.getClassPath.partition(_.startsWith(asProject(projectName).getLocationURI.getPath))
    import org.scalaide.lagom._
    val reloadableServerClasspath = mavenDeps(mavenDeps.defaultLocalRepoLocation(projectName))("com.lightbend.lagom", "lagom-reloadable-server_2.11", "1.3.5")
    val lagomConfig = new VMRunnerConfiguration(config.getClassToLaunch,
      addRunnerToClasspath(
        dependenciesPath,
        Seq("lagom-build-tool-support-1.3.5.jar",
          "better-files_2.11-2.17.1.jar",
          "play-file-watch_2.11-1.0.1.jar")
      ) ++ config.getBootClassPath ++ reloadableServerClasspath)
    lagomConfig.setBootClassPath(config.getBootClassPath)
    lagomConfig.setEnvironment(config.getEnvironment)
    lagomConfig.setProgramArguments(
      config.getProgramArguments ++
      Array(s"$LagomServicePathProgArgName${servicePath.mkString(File.pathSeparator)}",
        s"$LagomProjectProgArgName$projectName",
        s"$LagomServerPortProgArgName$serverPort",
        s"$LagomWorkDirProgArgName${config.getWorkingDirectory}",
        s"$LagomWatchTimeoutProgArgName$watchTimeout") ++
      optionalProgArgs(locatorPort, cassandraPort) ++
      srcsAndOutsProgArgs(projectName)
    )
    lagomConfig.setResumeOnStartup(config.isResumeOnStartup)
    lagomConfig.setVMArguments(config.getVMArguments)
    lagomConfig.setVMSpecificAttributesMap(config.getVMSpecificAttributesMap)
    lagomConfig.setWorkingDirectory(config.getWorkingDirectory)
    super.run(lagomConfig, launch, monitor)
  }
}

class LagomLaunchDelegate extends JavaLaunchDelegate with ClasspathGetterForLaunchDelegate
  with ProblemHandlersForLaunchDelegate with LagomScalaDebuggerForLaunchDelegate
