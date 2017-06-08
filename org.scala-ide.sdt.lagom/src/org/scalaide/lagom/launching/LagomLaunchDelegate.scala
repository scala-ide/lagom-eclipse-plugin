package org.scalaide.lagom.launching

import org.eclipse.core.runtime.IProgressMonitor
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
import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.FileLocator

trait LagomScalaDebuggerForLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
  override def getVMRunner(configuration: ILaunchConfiguration, mode: String): IVMRunner = {
    val vm = JavaRuntime.computeVMInstall(configuration)
    new LagomVMDebuggingRunner(vm)
  }
}

class LagomVMDebuggingRunner(vm: IVMInstall) extends StandardVMScalaDebugger(vm) with HasLogger {
  private def addLagomClass(lagomClass: String, programArgs: Array[String]): Array[String] =
    programArgs ++ Array(s"${LagomLauncher.LagomClassSwitch}$lagomClass")
  private def addRunnerToClasspath(classpath: Array[String]): Array[String] = {
    val lagomBundle = Platform.getBundle("org.scala-ide.sdt.lagom")
    val lagomLibPath = new Path("target/lib/lagomlauncher.jar")
    val lagomLib = FileLocator.find(lagomBundle, lagomLibPath, null)
    val fileLocation = FileLocator.toFileURL(lagomLib)
    val path = fileLocation.getPath
    classpath ++ Array(path)
  }
  override def run(config: VMRunnerConfiguration, launch: ILaunch, monitor: IProgressMonitor) = {
    val className = config.getClassToLaunch
    val lagomConfig = new VMRunnerConfiguration("org.scalaide.lagom.launching.LagomLauncher", addRunnerToClasspath(config.getClassPath))
    lagomConfig.setBootClassPath(config.getBootClassPath)
    lagomConfig.setEnvironment(config.getEnvironment)
    lagomConfig.setProgramArguments(addLagomClass(config.getClassToLaunch, config.getProgramArguments))
    lagomConfig.setResumeOnStartup(config.isResumeOnStartup)
    lagomConfig.setVMArguments(config.getVMArguments)
    lagomConfig.setVMSpecificAttributesMap(config.getVMSpecificAttributesMap)
    lagomConfig.setWorkingDirectory(config.getWorkingDirectory)
    super.run(lagomConfig, launch, monitor)
  }
}

class LagomLaunchDelegate extends JavaLaunchDelegate with ClasspathGetterForLaunchDelegate
  with ProblemHandlersForLaunchDelegate with LagomScalaDebuggerForLaunchDelegate
