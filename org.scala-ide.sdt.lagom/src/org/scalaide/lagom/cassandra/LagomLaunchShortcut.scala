package org.scalaide.lagom.cassandra

import scala.annotation.tailrec
import scala.reflect.NameTransformer
import scala.reflect.internal.util.BatchSourceFile

import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.debug.ui.ILaunchShortcut
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.internal.ui.actions.SelectionConverter
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.ISelectionProvider
import org.eclipse.jface.viewers.ITreeSelection
import org.eclipse.ui.IEditorPart
import org.scalaide.core.IScalaPlugin
import org.scalaide.core.compiler.IScalaPresentationCompiler.Implicits.RichResponse
import org.scalaide.core.internal.jdt.model.ScalaClassElement
import org.scalaide.core.internal.jdt.model.ScalaCompilationUnit
import org.scalaide.core.internal.jdt.model.ScalaElement
import org.scalaide.core.internal.jdt.model.ScalaSourceFile
import org.eclipse.core.resources.IProject

class LagomLaunchShortcut extends org.scalaide.lagom.LagomLaunchShortcut(LagomLaunchShortcut.launchLagom)

object LagomLaunchShortcut {
  private def getLaunchManager = DebugPlugin.getDefault.getLaunchManager

  def launchLagom(project: IProject, mode: String) {
    val configType = getLaunchManager.getLaunchConfigurationType("scalaide.lagom.cassandra")
    val existingConfigs = getLaunchManager.getLaunchConfigurations(configType)
    val existingConfigOpt = existingConfigs.find(config => config.getName == project.getName)
    val config = existingConfigOpt match {
      case Some(existingConfig) => existingConfig
      case None =>
        import LagomCassandraConfiguration._
        val wc = configType.newInstance(null, getLaunchManager.generateLaunchConfigurationName(LagomCassandraName))
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName)
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomCassandraRunnerClass)
        wc.setAttribute(LagomPort, LagomPortDefault)
        wc.setAttribute(LagomTimeout, LagomTimeoutDefault)
        wc.doSave
    }
    DebugUITools.launch(config, mode)
  }
}
