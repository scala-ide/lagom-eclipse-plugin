package org.scalaide.lagom

import org.eclipse.core.resources.IProject
import org.eclipse.debug.ui.ILaunchShortcut
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.internal.ui.actions.SelectionConverter
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.ISelectionProvider
import org.eclipse.jface.viewers.ITreeSelection
import org.eclipse.ui.IEditorPart

class LagomLaunchShortcut(launchLagom: (IProject, String) => Unit) extends ILaunchShortcut {

  private def actIfProject(mode: String, elem: IJavaElement): Unit = Option(elem.getJavaProject)
    .map { p => launchLagom(p.getProject, mode) }
    .orElse {
      MessageDialog.openError(null, "Error", "Underlying project not found.")
      None
    }

  def launch(selection: ISelection, mode: String): Unit = {
    selection match {
      case treeSelection: ITreeSelection =>
        treeSelection.getFirstElement match {
          case jElement: IJavaElement =>
            actIfProject(mode, jElement)
          case _ =>
            MessageDialog.openError(null, "Error", "Please select element in Lagom project.")
        }
      case _ =>
        MessageDialog.openError(null, "Error", "Please select element in Lagom project.")
    }
  }

  def launch(editorPart: IEditorPart, mode: String) {
    val typeRoot = JavaUI.getEditorInputTypeRoot(editorPart.getEditorInput())
    val selectionProvider: ISelectionProvider = editorPart.getSite().getSelectionProvider()
    if (selectionProvider != null) {
      val selection: ISelection = selectionProvider.getSelection()
      val element = SelectionConverter.getElementAtOffset(typeRoot, selection.asInstanceOf[ITextSelection])
      actIfProject(mode, element)
    } else
      MessageDialog.openError(null, "Error", "Please select element in Lagom project.")
  }
}
