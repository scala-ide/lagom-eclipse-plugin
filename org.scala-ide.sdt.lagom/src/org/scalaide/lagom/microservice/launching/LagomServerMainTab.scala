package org.scalaide.lagom.microservice.launching

import org.eclipse.jdt.debug.ui.launchConfigurations.JavaMainTab
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Group
import org.eclipse.debug.internal.ui.SWTFactory
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.ui.PlatformUI
import org.eclipse.jdt.internal.debug.ui.IJavaDebugHelpContextIds
import org.eclipse.swt.widgets.Text
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.jdt.internal.debug.ui.actions.ControlAccessibleListener
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.jdt.internal.debug.ui.launcher.SharedJavaMainTab
import org.eclipse.swt.graphics.Image
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jdt.ui.ISharedImages
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.eclipse.jdt.core.search.SearchEngine
import org.eclipse.jdt.internal.debug.ui.launcher.MainMethodSearchEngine
import org.eclipse.jdt.core.IType
import java.lang.reflect.InvocationTargetException
import org.eclipse.jdt.internal.debug.ui.launcher.DebugTypeSelectionDialog
import org.eclipse.jface.window.Window
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.core.resources.IWorkspace
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.resources.IResource
import com.ibm.icu.text.MessageFormat
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.core.runtime.CoreException
import org.scalaide.core.IScalaPlugin
import org.eclipse.core.runtime.IAdaptable
import org.scalaide.core.internal.jdt.model.ScalaSourceFile
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog
import org.eclipse.ui.dialogs.ResourceListSelectionDialog
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.ui.dialogs.ElementListSelectionDialog
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.core.resources.IProject
import org.eclipse.jface.viewers.TableViewer
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.TableColumn
import org.eclipse.swt.widgets.Table
import org.eclipse.jface.viewers.TableViewerColumn
import org.eclipse.swt.widgets.TableItem
import org.eclipse.swt.custom.TableEditor
import org.eclipse.swt.events.SelectionAdapter
import org.scalaide.core.IScalaProject
import org.eclipse.jdt.internal.debug.ui.launcher.AbstractJavaMainTab

class LagomServerMainTab extends AbstractJavaMainTab {
  // project location
  // source dirs
  // output dirs not necessary
  // play port number: 9099
  // service locator port: 8000
  // cassandra port: 4000
  // watch service timeout: 200
  // UI widgets
  private var mainGroup: Group = null
  private var fSearchButton: Button = null
  private var fSuiteRadioButton: Button = null
  private var fFileRadioButton: Button = null
  private var fPackageRadioButton: Button = null
  private var fIncludeNestedCheckBox: Button = null

  private var testNamesGroup: Group = null
  private var fTestNamesTable: Table = null
  private var fTestNamesEditor: TableEditor = null

  override def getId: String = "scalaide.lagom.microservice.tabGroup"
  override def getName: String = "Lagom Service"

  def createControl(parent: Composite) {
    val comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH)
    comp.getLayout.asInstanceOf[GridLayout].verticalSpacing = 0
    createProjectEditor(comp)
    createVerticalSpacer(comp, 1)
    createMainTypeEditor(comp, "Main Lagom Service Parameters")
    setControl(comp)
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaDebugHelpContextIds.LAUNCH_CONFIGURATION_DIALOG_MAIN_TAB)
  }

  private def createMainTypeEditor(parent: Composite, text: String) {
    val mainParamsGroup = SWTFactory.createGroup(parent, text, 2, 1, GridData.FILL_BOTH)
    createLines(mainParamsGroup)
  }

  protected def createLines(parent: Composite) {
    val text =""
    SWTFactory.createLabel(parent, "Lagom Server Port", 0)
    SWTFactory.createSingleText(parent, 0)
    SWTFactory.createLabel(parent, "Lagom Service Locator Port", 0)
    SWTFactory.createSingleText(parent, 0)
    SWTFactory.createLabel(parent, "Cassandra Port", 0)
    SWTFactory.createSingleText(parent, 0)
    SWTFactory.createLabel(parent, "Watch Service Timeout", 0)
    SWTFactory.createSingleText(parent, 0)
  }

  override def getImage = JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);

  override def isValid(config: ILaunchConfiguration): Boolean = {
    setErrorMessage(null)
    setMessage(null)
    var name = fProjText.getText().trim()
    if (name.length > 0) {
      val workspace = ResourcesPlugin.getWorkspace
      val status = workspace.validateName(name, IResource.PROJECT)
      if (status.isOK) {
        val project = ResourcesPlugin.getWorkspace.getRoot.getProject(name)
        if (!project.exists()) {
          setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_20, Array(name)))
          return false
        }
        if (!project.isOpen) {
          setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_21, Array(name)))
          return false
        }
      } else {
        setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_19, Array(status.getMessage())))
        return false
      }
    }
    return true
  }

  def performApply(config: ILaunchConfigurationWorkingCopy) {
    val configMap = new java.util.HashMap[String, Any]()
    configMap.put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText.trim)

    config.setAttributes(configMap)
    mapResources(config)
  }

  def setDefaults(config: ILaunchConfigurationWorkingCopy) {
    val javaElement = getContext
    if (javaElement != null)
      initializeJavaProject(javaElement, config)
    else
      config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
  }
}
