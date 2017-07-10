package org.scalaide.lagom.microservice.launching

import java.net.URL
import java.net.URLClassLoader

import org.eclipse.jdt.core.IJavaProject

import com.typesafe.config.ConfigFactory

object noLagomLoaderPath {
  import LagomServerConfiguration._
  def apply(javaProject: IJavaProject): Boolean = try {
    val projectLocation = javaProject.getProject.getLocation
    val ProjectNameSegment = 1
    val configClassLoader = new URLClassLoader(javaProject.getResolvedClasspath(true).flatMap { cp =>
      Option(cp.getOutputLocation)
    }.distinct.map { icp =>
      projectLocation.append(icp.removeFirstSegments(ProjectNameSegment)).toFile.toURI.toURL
    } ++ Option(javaProject.getOutputLocation).map { o =>
      projectLocation.append(o.removeFirstSegments(ProjectNameSegment)).toFile.toURI.toURL
    }.toArray[URL])
    val projectConfig = ConfigFactory.load(configClassLoader)
    !projectConfig.hasPath(LagomApplicationLoaderPath)
  } catch {
    case allowToFailInRunner: Throwable => true
  }
}
