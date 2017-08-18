package org.scalaide.lagom.locator

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchConfiguration
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
  private def emptyToNone(test: String) = if (test.isEmpty()) None else Option(test)
  import LagomLocatorConfiguration._
  override def run(config: VMRunnerConfiguration, launch: ILaunch, monitor: IProgressMonitor) = {
    val launchConfig = launch.getLaunchConfiguration
    val port = launchConfig.getAttribute(LagomPort, LagomPortDefault)
    val gateway = launchConfig.getAttribute(LagomGatewayPort, LagomGatewayPortDefault)
    val cass = emptyToNone(launchConfig.getAttribute(LagomCassandraPort, ""))
    val kafka = emptyToNone(launchConfig.getAttribute(LagomKafkaPort, ""))
    val progArgForCass = cass.fold(Array.empty[String])(cass => Array(s"$LagomCassandraPortProgArgName$cass"))
    val progArgForKafka = kafka.fold(Array.empty[String])(kafka => Array(s"$LagomKafkaPortProgArgName$kafka"))
    val projectName = launchConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
    import org.scalaide.lagom._
    val (scalaVersion, lagomVersion) = eclipseTools.findLagomVersion(eclipseTools.asProject(projectName))
    val locatorServerClasspath = eclipseTools.monitorLaunching(monitor, "Fetching Maven dependencies for Service Locator. It can take a while...") { 
      mavenDeps(mavenDeps.defaultLocalRepoLocation(projectName))("com.lightbend.lagom", s"lagom-service-locator_$scalaVersion", lagomVersion)
    }
    val lagomConfig = new VMRunnerConfiguration(config.getClassToLaunch,
      addRunnerToClasspath(config.getClassPath, scalaVersion) ++ config.getBootClassPath ++ locatorServerClasspath)
    lagomConfig.setBootClassPath(config.getBootClassPath)
    lagomConfig.setEnvironment(config.getEnvironment)
    lagomConfig.setProgramArguments(config.getProgramArguments ++
      Array(s"$LagomPortProgArgName$port",
        s"$LagomGatewayPortProgArgName$gateway") ++ progArgForCass ++ progArgForKafka)
    lagomConfig.setResumeOnStartup(config.isResumeOnStartup)
    lagomConfig.setVMArguments(config.getVMArguments)
    lagomConfig.setVMSpecificAttributesMap(config.getVMSpecificAttributesMap)
    lagomConfig.setWorkingDirectory(config.getWorkingDirectory)
    super.run(lagomConfig, launch, monitor)
  }
}

class LagomLaunchDelegate extends JavaLaunchDelegate with ClasspathGetterForLaunchDelegate
  with ProblemHandlersForLaunchDelegate with LagomScalaDebuggerForLaunchDelegate
