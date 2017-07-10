package org.scalaide.lagom.cassandra

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup
import org.eclipse.debug.ui.CommonTab
import org.eclipse.debug.ui.EnvironmentTab
import org.eclipse.debug.ui.ILaunchConfigurationDialog
import org.eclipse.debug.ui.ILaunchConfigurationTab
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab

class CassandraTabGroup extends AbstractLaunchConfigurationTabGroup {
  override def createTabs(dialog: ILaunchConfigurationDialog, mode: String) = {
    setTabs(Array[ILaunchConfigurationTab](
      new CassandraMainTab(),
      new JavaArgumentsTab(),
      new JavaClasspathTab(),
      new EnvironmentTab(),
      new CommonTab()))
  }
}