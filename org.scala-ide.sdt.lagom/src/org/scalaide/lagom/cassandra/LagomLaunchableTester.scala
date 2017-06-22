package org.scalaide.lagom.cassandra

import org.eclipse.core.expressions.PropertyTester
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.ui.part.FileEditorInput
import org.scalaide.core.internal.jdt.model.ScalaSourceFile
import org.eclipse.jdt.core.IJavaElement

class LagomLaunchableTester extends PropertyTester {
  private val PROPERTY_HAS_LAGOM_LOADER = "hasLagomLoader"

  def test(receiver: Object, property: String, args: Array[Object], expectedValue: Object): Boolean = isLagom(property){
    try {
      receiver match {
        case scSrcFile: ScalaSourceFile =>
          LagomLaunchShortcut.containsLagomLoaderClass(scSrcFile)
        case editorInput: FileEditorInput =>
          if(receiver.isInstanceOf[IAdaptable]) {
            val je = receiver.asInstanceOf[IAdaptable].getAdapter(classOf[IJavaElement]).asInstanceOf[IJavaElement]
            je.getOpenable match {
              case scSrcFile: ScalaSourceFile =>
                LagomLaunchShortcut.containsLagomLoaderClass(scSrcFile)
              case _ => false
            }
          }
          else false
        case _ => false
      }
    } catch {
      case _: Throwable => false
    }
  }

  private def isLagom(property: String)(tester: => Boolean) = property match {
    case PROPERTY_HAS_LAGOM_LOADER => tester
    case _ => false
  }
}