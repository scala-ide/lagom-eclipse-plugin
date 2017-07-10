package org.scalaide.lagom.microservice.launching

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup
import org.eclipse.debug.ui.CommonTab
import org.eclipse.debug.ui.EnvironmentTab
import org.eclipse.debug.ui.ILaunchConfigurationDialog
import org.eclipse.debug.ui.ILaunchConfigurationTab
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab

class LagomServerTabGroup extends AbstractLaunchConfigurationTabGroup {
  override def createTabs(dialog : ILaunchConfigurationDialog, mode : String) = {
    setTabs(Array[ILaunchConfigurationTab](
      new LagomServerMainTab(),
      new JavaArgumentsTab(),
      new JavaJRETab(),
      new JavaClasspathTab(),
      new SourceLookupTab(),
      new EnvironmentTab(),
      new CommonTab()
    ))
  }
}
