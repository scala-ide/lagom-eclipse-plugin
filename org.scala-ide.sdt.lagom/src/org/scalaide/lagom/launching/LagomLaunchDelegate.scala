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

trait LagomScalaDebuggerForLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
  override def getVMRunner(configuration: ILaunchConfiguration, mode: String): IVMRunner = {
    val vm = JavaRuntime.computeVMInstall(configuration)
    new LagomVMDebuggingRunner(vm)
  }
}

class LagomVMDebuggingRunner(vm: IVMInstall) extends StandardVMScalaDebugger(vm) {
  override def run(config: VMRunnerConfiguration, launch: ILaunch, monitor: IProgressMonitor) = {
    println(config.getClassToLaunch)
  }
}

class LagomLaunchDelegate extends JavaLaunchDelegate with ClasspathGetterForLaunchDelegate
  with ProblemHandlersForLaunchDelegate with LagomScalaDebuggerForLaunchDelegate
