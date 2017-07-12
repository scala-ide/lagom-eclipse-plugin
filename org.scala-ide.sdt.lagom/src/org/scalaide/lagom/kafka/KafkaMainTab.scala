package org.scalaide.lagom.kafka

import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.debug.internal.ui.SWTFactory
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

import com.ibm.icu.text.MessageFormat

class KafkaMainTab extends AbstractJavaMainTab {
  private var fPortText: Text = null
  private var fZookeeperPortText: Text = null

  override def getId: String = "scalaide.lagom.kafka.tabGroup"
  override def getName: String = "Lagom Kafka"

  def createControl(parent: Composite): Unit = {
    val comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH)
    comp.getLayout.asInstanceOf[GridLayout].verticalSpacing = 0
    createProjectEditor(comp)
    createVerticalSpacer(comp, 1)
    createMainTypeEditor(comp, "Main Lagom Kafka Parameters")
    setControl(comp)
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.LAUNCHER_CONFIGURATION)
  }

  private def createMainTypeEditor(parent: Composite, text: String): Unit = {
    val mainParamsGroup = SWTFactory.createGroup(parent, text, 2, 1, GridData.FILL_BOTH)
    createLines(mainParamsGroup)
    mainParamsGroup.pack()
  }

  protected def createLines(parent: Composite): Unit = {
    SWTFactory.createLabel(parent, "Kafka Server Port", 1)
    fPortText = SWTFactory.createSingleText(parent, 1)
    fPortText.addModifyListener(new ModifyListener() {
      override def modifyText(me: ModifyEvent): Unit = {
        scheduleUpdateJob()
      }
    })
    SWTFactory.createLabel(parent, "Kafka Zookeeper Port", 1)
    fZookeeperPortText = SWTFactory.createSingleText(parent, 1)
    fZookeeperPortText.addModifyListener(new ModifyListener() {
      override def modifyText(me: ModifyEvent): Unit = {
        scheduleUpdateJob()
      }
    })
  }

  private val image = Option(LagomImages.LAGOM_KAFKA_SERVER.createImage)
  override def getImage = image.getOrElse(null)

  override def dispose: Unit = {
    image.foreach(_.dispose())
  }

  import LagomKafkaConfiguration._

  private def settingsValidator: Boolean =
    if (!fPortText.getText.trim.forall(Character.isDigit)) {
      setErrorMessage(s"Kafka port must be a number.")
      false
    } else if (!fZookeeperPortText.getText.trim.forall(Character.isDigit)) {
      setErrorMessage(s"Zookeeper port must be a number.")
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
        import org.scalaide.lagom.projectValidator
        projectValidator(fProjText.getText.trim, setErrorMessage)(project) && settingsValidator
      } else {
        setErrorMessage(MessageFormat.format(LauncherMessages.JavaMainTab_19, Array(status.getMessage())))
        false
      }
    } else
      true
  }

  override def initializeFrom(configuration: ILaunchConfiguration): Unit = {
    super.initializeFrom(configuration)
    fPortText.setText(configuration.getAttribute(LagomPort, LagomPortDefault))
    fZookeeperPortText.setText(configuration.getAttribute(LagomZookeeperPort, LagomZookeeperPortDefault))
  }

  def performApply(config: ILaunchConfigurationWorkingCopy): Unit = {
    val configMap = new java.util.HashMap[String, Any]()
    configMap.put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText.trim)
    configMap.put(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomKafkaRunnerClass)
    configMap.put(LagomPort, fPortText.getText.trim)
    configMap.put(LagomZookeeperPort, fZookeeperPortText.getText.trim)
    config.setAttributes(configMap)
    mapResources(config)
  }

  def setDefaults(config: ILaunchConfigurationWorkingCopy): Unit = {
    val javaElement = getContext
    if (javaElement != null)
      initializeJavaProject(javaElement, config)
    else
      config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomKafkaRunnerClass)
    config.setAttribute(LagomPort, LagomPortDefault)
    config.setAttribute(LagomZookeeperPort, LagomZookeeperPortDefault)
  }
}