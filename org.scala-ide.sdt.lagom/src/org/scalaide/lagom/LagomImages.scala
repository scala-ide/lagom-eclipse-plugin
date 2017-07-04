package org.scalaide.lagom

import java.net.MalformedURLException
import java.net.URL

import org.eclipse.core.runtime.Platform
import org.eclipse.jface.resource.ImageDescriptor
import org.scalaide.ui.ScalaImages

object LagomImages {

  val LAGOM_LAGOM_SERVER = create("icons/full/obj16/littlelagom.png")
  val LAGOM_LOCATOR_SERVER = create("icons/full/obj16/lagomlocator.png")
  val LAGOM_CASSANDRA_SERVER = create("icons/full/obj16/lagomcassandra.png")

  private def create(localPath: String) = {
    try {
      val pluginInstallUrl = Platform.getBundle("org.scala-ide.sdt.lagom").getEntry("/")
      val url = new URL(pluginInstallUrl, localPath)
      ImageDescriptor.createFromURL(url)
    } catch {
      case _: MalformedURLException =>
        ScalaImages.MISSING_ICON
    }
  }

}