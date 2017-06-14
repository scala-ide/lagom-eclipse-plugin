package org.scalaide.lagom.microservice.launching

import org.eclipse.debug.ui._
import org.eclipse.debug.ui.sourcelookup._
import org.eclipse.jdt.debug.ui.launchConfigurations._

class ScalaTestTabGroup extends AbstractLaunchConfigurationTabGroup {
  override def createTabs(dialog : ILaunchConfigurationDialog, mode : String) = {
    setTabs(Array[ILaunchConfigurationTab](
      new ScalaTestMainTab(),
      new JavaArgumentsTab(),
      new JavaJRETab(),
      new JavaClasspathTab(),
      new SourceLookupTab(),
      new EnvironmentTab(),
      new CommonTab()
    ))
  }
}