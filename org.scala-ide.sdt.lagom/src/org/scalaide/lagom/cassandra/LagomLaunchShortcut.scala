package org.scalaide.lagom.cassandra

import org.eclipse.core.resources.IProject
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants

class LagomLaunchShortcut extends org.scalaide.lagom.LagomLaunchShortcut(LagomLaunchShortcut.launchLagom)

object LagomLaunchShortcut {
  private def getLaunchManager = DebugPlugin.getDefault.getLaunchManager

  def launchLagom(project: IProject, mode: String) {
    val configType = getLaunchManager.getLaunchConfigurationType("scalaide.lagom.cassandra")
    val existingConfigs = getLaunchManager.getLaunchConfigurations(configType)
    val existingConfigOpt = existingConfigs.find(config => config.getName == project.getName)
    val config = existingConfigOpt match {
      case Some(existingConfig) => existingConfig
      case None =>
        import LagomCassandraConfiguration._
        val wc = configType.newInstance(null, getLaunchManager.generateLaunchConfigurationName(LagomCassandraName))
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName)
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomCassandraRunnerClass)
        wc.setAttribute(LagomPort, LagomPortDefault)
        wc.setAttribute(LagomTimeout, LagomTimeoutDefault)
        wc.doSave
    }
    DebugUITools.launch(config, mode)
  }
}
