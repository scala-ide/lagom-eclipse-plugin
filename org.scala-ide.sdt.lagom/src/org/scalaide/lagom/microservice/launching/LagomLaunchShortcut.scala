package org.scalaide.lagom.microservice.launching

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

class LagomLaunchShortcut extends ILaunchShortcut {
  import LagomLaunchShortcut._

  def launch(selection: ISelection, mode: String): Unit = {
    selection match {
      case treeSelection: ITreeSelection =>
        treeSelection.getFirstElement match {
          case scSrcFile: ScalaSourceFile =>
            scSrcFile.getChildren.collect {
              case child => getLagomLoaderClass(child)
            }.collectFirst {
              case Some(classElement) =>
                classElement
            }.map {
              launchLagom(_, mode)
            }.orElse {
              MessageDialog.openError(null, "Error", "Please select Lagom application loader class to launch.")
              None
            }
          case classElement: ScalaClassElement =>
            getLagomLoaderClass(classElement).map {
              launchLagom(_, mode)
            }.orElse {
              MessageDialog.openError(null, "Error", "Please select Lagom application loader class to launch.")
              None
            }
          case _ =>
            MessageDialog.openError(null, "Error", "Please select Lagom application loader class to launch.")
        }
      case _ =>
        MessageDialog.openError(null, "Error", "Please select Lagom application loader class to launch.")
    }
  }

  def launch(editorPart: IEditorPart, mode: String) {
    // This get called when user right-clicked within the opened file editor and choose 'Run As' -> ScalaTest
    val typeRoot = JavaUI.getEditorInputTypeRoot(editorPart.getEditorInput())
    val selectionProvider: ISelectionProvider = editorPart.getSite().getSelectionProvider()
    if (selectionProvider != null) {
      val selection: ISelection = selectionProvider.getSelection()
      val element = SelectionConverter.getElementAtOffset(typeRoot, selection.asInstanceOf[ITextSelection])
      val classElementOpt = LagomLaunchShortcut.getLagomLoaderClass(element)
      classElementOpt match {
        case Some(classElement) =>
          launchLagom(classElement, mode)
        case None =>
          MessageDialog.openError(null, "Error", "Please select a ScalaTest suite to launch.")
      }
    } else
      MessageDialog.openError(null, "Error", "Please select a ScalaTest suite to launch.")
  }
}

object LagomLaunchShortcut {

  private def isLagomApplicationLoader(iType: IType): Boolean = {
    if (iType.isClass) {
      val project = iType.getJavaProject.getProject
      val scProject = IScalaPlugin().getScalaProject(project)
      scProject.presentationCompiler { compiler =>
        import compiler._
        val scu = iType.getCompilationUnit.asInstanceOf[ScalaCompilationUnit]
        val response = new Response[Tree]
        compiler.askParsedEntered(new BatchSourceFile(scu.file, scu.getContents), false, response)
        response.getOption().map { tree =>
          tree.children.exists {
            case classDef: ClassDef if classDef.symbol.fullName == iType.getFullyQualifiedName =>
              val linearizedBaseClasses = compiler.asyncExec { classDef.symbol.info.baseClasses }.getOrElse(List.empty)()
              linearizedBaseClasses.exists {
                _.fullName == "com.lightbend.lagom.scaladsl.server.LagomApplicationLoader"
              }
            case _ =>
              false
          }
        }
      }.flatten.getOrElse(false)
    } else
      false
  }

  def containsLagomLoaderClass(scSrcFile: ScalaSourceFile): Boolean = {
    val lagomOpt = scSrcFile.getAllTypes().find { tpe => isLagomApplicationLoader(tpe) }
    lagomOpt match {
      case Some(lagom) => true
      case None        => false
    }
  }

  private def getLagomLoaderClass(element: IJavaElement): Option[ScalaClassElement] = {
    element match {
      case scElement: ScalaElement =>
        val classElement = LagomLaunchShortcut.getClassElement(element)
        if (classElement != null && LagomLaunchShortcut.isLagomApplicationLoader(classElement))
          Some(classElement)
        else
          None
      case _ =>
        None
    }
  }

  @tailrec
  private def getClassElement(element: IJavaElement): ScalaClassElement = {
    element match {
      case scClassElement: ScalaClassElement =>
        scClassElement
      case _ =>
        if (element.getParent != null)
          getClassElement(element.getParent)
        else
          null
    }
  }

  private def getLaunchManager = DebugPlugin.getDefault.getLaunchManager

  private def launchLagom(classElement: ScalaClassElement, mode: String) {
    val configType = getLaunchManager.getLaunchConfigurationType("scalaide.lagom.microservice")
    val existingConfigs = getLaunchManager.getLaunchConfigurations(configType)
    val simpleName = NameTransformer.decode(classElement.labelName)
    val existingConfigOpt = existingConfigs.find(config => config.getName == simpleName)
    val config = existingConfigOpt match {
      case Some(existingConfig) => existingConfig
      case None =>
        val wc = configType.newInstance(null, getLaunchManager.generateLaunchConfigurationName(simpleName.replaceAll(":", "-").replaceAll("\"", "'")))
        val project = classElement.getJavaProject.getProject
        import LagomServerConfiguration._
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, LagomServiceRunnerClass)
        wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName)
        wc.setAttribute(LagomServerPort, LagomServerPortDefault)
        wc.setAttribute(LagomLocatorPort, LagomLocatorPortDefault)
        wc.setAttribute(LagomCassandraPort, LagomCassandraPortDefault)
        wc.setAttribute(LagomWatchTimeout, LagomWatchTimeoutDefault)
        wc.doSave
    }
    DebugUITools.launch(config, mode)
  }
}
