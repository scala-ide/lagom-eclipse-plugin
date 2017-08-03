package org.scalaide.lagom.microservice

import org.eclipse.core.resources.IProject
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.scalaide.lagom.cassandra.LagomCassandraConfiguration
import org.scalaide.lagom.locator.LagomLocatorConfiguration

class LagomLaunchShortcut extends org.scalaide.lagom.LagomLaunchShortcut(LagomLaunchShortcut.launchLagom)

object LagomLaunchShortcut {
  private def getLaunchManager = DebugPlugin.getDefault.getLaunchManager

  def launchLagom(project: IProject, mode: String): Unit = {
    val configType = getLaunchManager.getLaunchConfigurationType("scalaide.lagom.microservice")
    val existingConfigs = getLaunchManager.getLaunchConfigurations(configType)
    val existingConfigOpt = existingConfigs.find { config =>
      config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "") == project.getName
    }
    val config = existingConfigOpt match {
      case Some(existingConfig) => existingConfig
      case None =>
        val wc = configType.newInstance(null, getLaunchManager.generateLaunchConfigurationName(project.getName))
        import LagomServerConfiguration._
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomServiceRunnerClass)
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName)
        wc.setAttribute(LagomServerPort, LagomServerPortDefault)
        wc.setAttribute(LagomLocatorPort, LagomLocatorConfiguration.LagomPortDefault)
        wc.setAttribute(LagomCassandraPort, LagomCassandraConfiguration.LagomPortDefault)
        wc.setAttribute(LagomWatchTimeout, LagomWatchTimeoutDefault)
        wc.doSave
    }
    DebugUITools.launch(config, mode)
  }
}
