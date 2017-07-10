package org.scalaide.lagom.microservice.launching

import org.eclipse.core.expressions.PropertyTester
import org.eclipse.jdt.core.IJavaElement

class LagomLaunchableTester extends PropertyTester {
  private val PROPERTY_HAS_LAGOM_LOADER = "hasLagomLoader"

  def test(receiver: Object, property: String, args: Array[Object], expectedValue: Object): Boolean = isLagom(property) {
    try {
      receiver match {
        case javaElement: IJavaElement =>
          Option(javaElement.getJavaProject).map(!noLagomLoaderPath(_)).getOrElse(false)
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