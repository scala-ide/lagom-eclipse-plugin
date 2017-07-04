package org.scalaide.lagom.microservice.launching

import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.debug.internal.ui.SWTFactory
import org.eclipse.jdt.internal.debug.ui.launcher.AbstractJavaMainTab
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.pde.internal.ui.IHelpContextIds
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Text
import org.eclipse.ui.PlatformUI
import org.scalaide.lagom.LagomImages

import com.ibm.icu.text.MessageFormat
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.events.ModifyEvent

class LagomServerMainTab extends AbstractJavaMainTab {
  // project location
  // source dirs
  // output dirs not necessary
  // play port number: 9099
  // service locator port: 8000
  // cassandra port: 4000
  // watch service timeout: 200
  private var fLagomPortText: Text = null
  private var fLocatorPortText: Text = null
  private var fCassandraPortText: Text = null
  private var fWatchTimeoutText: Text = null

  override def getId: String = "scalaide.lagom.microservice.tabGroup"
  override def getName: String = "Lagom Service"

  def createControl(parent: Composite) {
    val comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH)
    comp.getLayout.asInstanceOf[GridLayout].verticalSpacing = 0
    createProjectEditor(comp)
    createVerticalSpacer(comp, 1)
    createMainTypeEditor(comp, "Main Lagom Service Parameters")
    setControl(comp)
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.LAUNCHER_CONFIGURATION)
  }

  private def createMainTypeEditor(parent: Composite, text: String) {
    val mainParamsGroup = SWTFactory.createGroup(parent, text, 2, 1, GridData.FILL_BOTH)
    createLines(mainParamsGroup)
    mainParamsGroup.pack()
  }

  protected def createLines(parent: Composite) {
    SWTFactory.createLabel(parent, "Lagom Server Port", 1)
    fLagomPortText = SWTFactory.createSingleText(parent, 1)
    fLagomPortText.addModifyListener(new ModifyListener() {
      override def modifyText(me: ModifyEvent): Unit = {
        scheduleUpdateJob()
      }
    })
    SWTFactory.createLabel(parent, "Lagom Service Locator Port", 1)
    fLocatorPortText = SWTFactory.createSingleText(parent, 1)
    fLocatorPortText.addModifyListener(new ModifyListener() {
      override def modifyText(me: ModifyEvent): Unit = {
        scheduleUpdateJob()
      }
    })
    SWTFactory.createLabel(parent, "Cassandra Port", 1)
    fCassandraPortText = SWTFactory.createSingleText(parent, 1)
    fCassandraPortText.addModifyListener(new ModifyListener() {
      override def modifyText(me: ModifyEvent): Unit = {
        print(me.data)
        scheduleUpdateJob()
      }
    })
    SWTFactory.createLabel(parent, "Watch Service Timeout [ms]", 1)
    fWatchTimeoutText = SWTFactory.createSingleText(parent, 1)
    fWatchTimeoutText.addModifyListener(new ModifyListener() {
      override def modifyText(me: ModifyEvent): Unit = {
        scheduleUpdateJob()
      }
    })
  }

  private val image = Option(LagomImages.LAGOM_LAGOM_SERVER.createImage)
  override def getImage = image.getOrElse(null)

  override def dispose = {
    image.foreach(_.dispose())
  }

  override def isValid(config: ILaunchConfiguration): Boolean = {
    setErrorMessage(null)
    setMessage(null)
    var name = fProjText.getText.trim
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

  import LagomServerConfiguration._
  override def initializeFrom(configuration: ILaunchConfiguration): Unit = {
    super.initializeFrom(configuration)
    fLagomPortText.setText(configuration.getAttribute(LagomServerPort, 9099.toString))
    fLocatorPortText.setText(configuration.getAttribute(LagomLocatorPort, 8000.toString))
    fCassandraPortText.setText(configuration.getAttribute(LagomCassandraPort, 4000.toString))
    fWatchTimeoutText.setText(configuration.getAttribute(LagomWatchTimeout, 200.toString))
  }

  def performApply(config: ILaunchConfigurationWorkingCopy) {
    val configMap = new java.util.HashMap[String, Any]()
    configMap.put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText.trim)
    configMap.put(LagomServerPort, fLagomPortText.getText.trim)
    configMap.put(LagomLocatorPort, fLocatorPortText.getText.trim)
    configMap.put(LagomCassandraPort, fCassandraPortText.getText.trim)
    configMap.put(LagomWatchTimeout, fWatchTimeoutText.getText.trim)
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
