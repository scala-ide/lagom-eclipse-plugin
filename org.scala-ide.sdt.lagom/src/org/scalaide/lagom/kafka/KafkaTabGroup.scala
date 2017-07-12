package org.scalaide.lagom.kafka

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup
import org.eclipse.debug.ui.CommonTab
import org.eclipse.debug.ui.EnvironmentTab
import org.eclipse.debug.ui.ILaunchConfigurationDialog
import org.eclipse.debug.ui.ILaunchConfigurationTab
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab

class KafkaTabGroup extends AbstractLaunchConfigurationTabGroup {
  override def createTabs(dialog: ILaunchConfigurationDialog, mode: String) = {
    setTabs(Array[ILaunchConfigurationTab](
      new KafkaMainTab(),
      new JavaArgumentsTab(),
      new JavaClasspathTab(),
      new EnvironmentTab(),
      new CommonTab()))
  }
}
