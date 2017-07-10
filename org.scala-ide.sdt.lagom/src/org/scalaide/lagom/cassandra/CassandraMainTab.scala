package org.scalaide.lagom.cassandra

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.debug.internal.ui.SWTFactory
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.debug.ui.launcher.AbstractJavaMainTab
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.pde.internal.ui.IHelpContextIds
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Text
import org.eclipse.ui.PlatformUI
import org.scalaide.lagom.LagomImages
import org.scalaide.lagom.microservice.launching.LagomServerConfiguration

import com.ibm.icu.text.MessageFormat

class CassandraMainTab extends AbstractJavaMainTab {
  private var fPortText: Text = null
  private var fTimeoutText: Text = null

  override def getId: String = "scalaide.lagom.cassandra.tabGroup"
  override def getName: String = "Lagom Cassandra"

  def createControl(parent: Composite): Unit = {
    val comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH)
    comp.getLayout.asInstanceOf[GridLayout].verticalSpacing = 0
    createProjectEditor(comp)
    createVerticalSpacer(comp, 1)
    createMainTypeEditor(comp, "Main Lagom Cassandra Parameters")
    setControl(comp)
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.LAUNCHER_CONFIGURATION)
  }

  private def createMainTypeEditor(parent: Composite, text: String): Unit = {
    val mainParamsGroup = SWTFactory.createGroup(parent, text, 2, 1, GridData.FILL_BOTH)
    createLines(mainParamsGroup)
    mainParamsGroup.pack()
  }

  protected def createLines(parent: Composite): Unit = {
    SWTFactory.createLabel(parent, "Cassandra Server Port", 1)
    fPortText = SWTFactory.createSingleText(parent, 1)
    fPortText.addModifyListener(new ModifyListener() {
      override def modifyText(me: ModifyEvent): Unit = {
        scheduleUpdateJob()
      }
    })
    SWTFactory.createLabel(parent, "Cassandra Startup Timeout [ms]", 1)
    fTimeoutText = SWTFactory.createSingleText(parent, 1)
    fTimeoutText.addModifyListener(new ModifyListener() {
      override def modifyText(me: ModifyEvent): Unit = {
        scheduleUpdateJob()
      }
    })
  }

  private val image = Option(LagomImages.LAGOM_CASSANDRA_SERVER.createImage)
  override def getImage = image.getOrElse(null)

  override def dispose: Unit = {
    image.foreach(_.dispose())
  }

  import LagomCassandraConfiguration._

  private def projectValidator: PartialFunction[IProject, Boolean] = {
    val name = fProjText.getText.trim
    PartialFunction.empty[IProject, Boolean].orElse[IProject, Boolean] {
      case project if !project.exists() =>
        setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_20, Array(name)))
        false
    }.orElse[IProject, Boolean] {
      case project if !project.isOpen =>
        setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_21, Array(name)))
        false
    }.orElse[IProject, Boolean] {
      case project if !project.hasNature(JavaCore.NATURE_ID) =>
        setErrorMessage(s"Project $name does not have Java nature.")
        false
    }.orElse[IProject, Boolean] {
      case project if noLagomLoaderPathInConfig(project) =>
        setErrorMessage(s"Project $name does not define ${LagomServerConfiguration.LagomApplicationLoaderPath} path in configuration.")
        false
    }.orElse[IProject, Boolean] {
      case _ =>
        true
    }
  }

  private def settingsValidator: Boolean =
    if (!fPortText.getText.trim.forall(Character.isDigit)) {
      setErrorMessage(s"Cassandra port must be a number.")
      false
    } else if (!fTimeoutText.getText.trim.forall(Character.isDigit)) {
      setErrorMessage(s"Startup timeout must be a number.")
      false
    } else true

  override def isValid(config: ILaunchConfiguration): Boolean = {
    setErrorMessage(null)
    setMessage(null)
    var name = fProjText.getText.trim
    if (name.length > 0) {
      val workspace = ResourcesPlugin.getWorkspace
      val status = workspace.validateName(name, IResource.PROJECT)
      if (status.isOK) {
        val project = ResourcesPlugin.getWorkspace.getRoot.getProject(name)
        projectValidator(project) && settingsValidator
      } else {
        setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_19, Array(status.getMessage())))
        false
      }
    } else
      true
  }

  import org.scalaide.lagom.noLagomLoaderPath
  private val noLagomLoaderPathInConfig: IProject => Boolean = (noLagomLoaderPath.apply _) compose (JavaCore.create _)
  
  override def initializeFrom(configuration: ILaunchConfiguration): Unit = {
    super.initializeFrom(configuration)
    fPortText.setText(configuration.getAttribute(LagomPort, LagomPortDefault))
    fTimeoutText.setText(configuration.getAttribute(LagomTimeout, LagomTimeoutDefault))
  }

  private def setIfSet(key: String, value: String, configMap: scala.collection.mutable.Map[String, Any]): Unit =
    if (value.nonEmpty) configMap += (key -> value)

  def performApply(config: ILaunchConfigurationWorkingCopy): Unit = {
    val configMap = new java.util.HashMap[String, Any]()
    configMap.put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText.trim)
    configMap.put(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomCassandraRunnerClass)
    configMap.put(LagomPort, fPortText.getText.trim)
    configMap.put(LagomTimeout, fTimeoutText.getText.trim)
    config.setAttributes(configMap)
    mapResources(config)
  }

  def setDefaults(config: ILaunchConfigurationWorkingCopy): Unit = {
    val javaElement = getContext
    if (javaElement != null)
      initializeJavaProject(javaElement, config)
    else
      config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomCassandraRunnerClass)
    config.setAttribute(LagomPort, LagomPortDefault)
    config.setAttribute(LagomTimeout, LagomTimeoutDefault)
  }
}
