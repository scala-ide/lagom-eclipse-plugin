package org.scalaide.lagom.locator.launching

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
import scala.collection.Seq

trait LagomScalaDebuggerForLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {
  override def getVMRunner(configuration: ILaunchConfiguration, mode: String): IVMRunner = {
    val vm = JavaRuntime.computeVMInstall(configuration)
    new LagomVMDebuggingRunner(vm)
  }
}

class LagomVMDebuggingRunner(vm: IVMInstall) extends StandardVMScalaDebugger(vm) with HasLogger {
  private def addLagomClass(lagomClass: String, programArgs: Array[String]): Array[String] =
    programArgs ++ Array(s"lagomclass$lagomClass")
  private def addRunnerToClasspath(classpath: Array[String]): Array[String] = {
    val lagomBundle = Platform.getBundle("org.scala-ide.sdt.lagom")
    def findPath(lib: String) = {
      val libPath = new Path(s"runner-libs/$lib")
      val libBundleLocation = FileLocator.find(lagomBundle, libPath, null)
      val libFile = FileLocator.toFileURL(libBundleLocation)
      libFile.getPath
    }
    val paths = Seq("org.scala-ide.sdt.lagom.runner-1.0.0-SNAPSHOT.jar",
      "lagom-service-locator_2.11-1.3.4.jar",
      "lagom-service-registry-client_2.11-1.3.4.jar",
      "lagom-javadsl-server_2.11-1.3.4.jar",
      "lagom-javadsl-client_2.11-1.3.4.jar",
      "lagom-javadsl-api_2.11-1.3.4.jar",
      "lagom-javadsl-jackson_2.11-1.3.4.jar",
      "jackson-module-parameter-names-2.7.8.jar",
      "jackson-datatype-guava-2.7.8.jar",
      "jackson-datatype-pcollections-2.7.8.jar",
      "pcollections-2.1.2.jar"
    ).map(findPath)
    classpath ++ paths
  }
  override def run(config: VMRunnerConfiguration, launch: ILaunch, monitor: IProgressMonitor) = {
    val className = config.getClassToLaunch
    val lagomConfig = new VMRunnerConfiguration("org.scalaide.lagom.locator.LagomLauncher", addRunnerToClasspath(config.getClassPath) ++ config.getBootClassPath)
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
