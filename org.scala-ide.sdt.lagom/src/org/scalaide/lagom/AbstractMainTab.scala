package org.scalaide.lagom

import java.text.MessageFormat

import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.jdt.internal.debug.ui.launcher.AbstractJavaMainTab
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants

abstract class AbstractMainTab extends AbstractJavaMainTab {
  def isValid(settingsValidator: => Boolean)(config: ILaunchConfiguration): Boolean = {
    setErrorMessage(null)
    setMessage(null)
    var name = fProjText.getText.trim
    if (name.length > 0) {
      val workspace = ResourcesPlugin.getWorkspace
      val status = workspace.validateName(name, IResource.PROJECT)
      if (status.isOK) {
        val project = ResourcesPlugin.getWorkspace.getRoot.getProject(name)
        import org.scalaide.lagom.projectValidator
        projectValidator(fProjText.getText.trim, setErrorMessage)(project) && settingsValidator
      } else {
        setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_19, Array(status.getMessage())))
        false
      }
    } else
      true
  }

  def setProjectName(config: ILaunchConfigurationWorkingCopy) = {
    val javaElement = getContext
    if (javaElement != null)
      initializeJavaProject(javaElement, config)
    else
      config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
  }
}
