package org.scalaide.lagom.locator

import org.eclipse.core.resources.IProject
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.scalaide.lagom.cassandra.LagomCassandraConfiguration
import org.scalaide.lagom.kafka.LagomKafkaConfiguration

class LagomLaunchShortcut extends org.scalaide.lagom.LagomLaunchShortcut(LagomLaunchShortcut.launchLagom)

object LagomLaunchShortcut {

  def getLaunchManager = DebugPlugin.getDefault.getLaunchManager

  def launchLagom(project: IProject, mode: String) {
    val configType = getLaunchManager.getLaunchConfigurationType("scalaide.lagom.locator")
    val existingConfigs = getLaunchManager.getLaunchConfigurations(configType)
    val existingConfigOpt = existingConfigs.find { config =>
      config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "") == project.getName
    }
    val config = existingConfigOpt match {
      case Some(existingConfig) => existingConfig
      case None =>
        import LagomLocatorConfiguration._
        val wc = configType.newInstance(null, getLaunchManager.generateLaunchConfigurationName(LagomLocatorName))
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomLocatorRunnerClass)
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName)
        wc.setAttribute(LagomPort, LagomPortDefault)
        wc.setAttribute(LagomGatewayPort, LagomGatewayPortDefault)
        wc.setAttribute(LagomCassandraPort, LagomCassandraConfiguration.LagomPortDefault)
        wc.setAttribute(LagomKafkaPort, LagomKafkaConfiguration.LagomPortDefault)
        wc.doSave
    }
    DebugUITools.launch(config, mode)
  }
}
