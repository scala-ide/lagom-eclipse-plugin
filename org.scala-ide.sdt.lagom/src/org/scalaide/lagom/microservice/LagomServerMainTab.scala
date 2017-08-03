package org.scalaide.lagom.microservice

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.debug.internal.ui.SWTFactory
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.pde.internal.ui.IHelpContextIds
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Text
import org.eclipse.ui.PlatformUI
import org.scalaide.lagom.AbstractMainTab
import org.scalaide.lagom.LagomImages
import org.scalaide.lagom.locator.LagomLocatorConfiguration
import org.scalaide.lagom.cassandra.LagomCassandraConfiguration

class LagomServerMainTab extends AbstractMainTab {
  private var fLagomPortText: Text = null
  private var fLocatorPortText: Text = null
  private var fCassandraPortText: Text = null
  private var fWatchTimeoutText: Text = null

  override def getId: String = "scalaide.lagom.microservice.tabGroup"
  override def getName: String = "Lagom Service"

  def createControl(parent: Composite): Unit = {
    val comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH)
    comp.getLayout.asInstanceOf[GridLayout].verticalSpacing = 0
    createProjectEditor(comp)
    createVerticalSpacer(comp, 1)
    createMainTypeEditor(comp, "Main Lagom Service Parameters")
    setControl(comp)
    PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.LAUNCHER_CONFIGURATION)
  }

  private def createMainTypeEditor(parent: Composite, text: String): Unit = {
    val mainParamsGroup = SWTFactory.createGroup(parent, text, 2, 1, GridData.FILL_BOTH)
    createLines(mainParamsGroup)
    mainParamsGroup.pack()
  }

  protected def createLines(parent: Composite): Unit = {
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

  override def dispose: Unit = {
    image.foreach(_.dispose())
  }

  import LagomServerConfiguration._

  private def settingsValidator: Boolean =
    if (!fCassandraPortText.getText.trim.forall(Character.isDigit)) {
      setErrorMessage(s"Cassandra port must be a number.")
      false
    } else if (!fLagomPortText.getText.trim.forall(Character.isDigit)) {
      setErrorMessage(s"Lagom Server port must be a number.")
      false
    } else if (!fLocatorPortText.getText.trim.forall(Character.isDigit)) {
      setErrorMessage(s"Service Locator port must be a number.")
      false
    } else if (!fWatchTimeoutText.getText.trim.forall(Character.isDigit)) {
      setErrorMessage(s"Timeout must be a number.")
      false
    } else true

  override def isValid(config: ILaunchConfiguration): Boolean =
    isValid(settingsValidator)(config)

  override def initializeFrom(configuration: ILaunchConfiguration): Unit = {
    super.initializeFrom(configuration)
    fLagomPortText.setText(configuration.getAttribute(LagomServerPort, LagomServerPortDefault))
    fLocatorPortText.setText(configuration.getAttribute(LagomLocatorPort, LagomLocatorConfiguration.LagomPortDefault))
    fCassandraPortText.setText(configuration.getAttribute(LagomCassandraPort, LagomCassandraConfiguration.LagomPortDefault))
    fWatchTimeoutText.setText(configuration.getAttribute(LagomWatchTimeout, LagomWatchTimeoutDefault))
  }

  private def putIfSet(key: String, value: String, configMap: scala.collection.mutable.Map[String, Any]): Unit =
    if (value.nonEmpty) configMap += (key -> value)

  def performApply(config: ILaunchConfigurationWorkingCopy): Unit = {
    import scala.collection.JavaConverters._
    val configMap = new java.util.HashMap[String, Any]()
    configMap.put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProjText.getText.trim)
    configMap.put(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomServiceRunnerClass)
    putIfSet(LagomServerPort, fLagomPortText.getText.trim, configMap.asScala)
    configMap.put(LagomLocatorPort, fLocatorPortText.getText.trim)
    configMap.put(LagomCassandraPort, fCassandraPortText.getText.trim)
    putIfSet(LagomWatchTimeout, fWatchTimeoutText.getText.trim, configMap.asScala)
    config.setAttributes(configMap)
    mapResources(config)
  }

  def setDefaults(config: ILaunchConfigurationWorkingCopy): Unit = {
    setProjectName(config)
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomServiceRunnerClass)
    config.setAttribute(LagomServerPort, LagomServerPortDefault)
    config.setAttribute(LagomLocatorPort, LagomLocatorConfiguration.LagomPortDefault)
    config.setAttribute(LagomCassandraPort, LagomCassandraConfiguration.LagomPortDefault)
    config.setAttribute(LagomWatchTimeout, LagomWatchTimeoutDefault)
  }
}
