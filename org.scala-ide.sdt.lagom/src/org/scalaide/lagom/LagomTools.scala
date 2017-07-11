package org.scalaide.lagom

import java.net.URL
import java.net.URLClassLoader

import org.eclipse.jdt.core.IJavaProject

import com.typesafe.config.ConfigFactory
import org.eclipse.core.resources.IProject
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages
import java.text.MessageFormat
import org.eclipse.jdt.core.JavaCore

object noLagomLoaderPath {
  import org.scalaide.lagom.microservice.launching.LagomServerConfiguration._
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

object projectValidator {
  val LagomApplicationLoaderPath = "play.application.loader"

  private val noLagomLoaderPathInConfig: IProject => Boolean = (noLagomLoaderPath.apply _) compose (JavaCore.create _)

  def apply(name: String, setErrorMessage: String => Unit): PartialFunction[IProject, Boolean] = {
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
        setErrorMessage(s"Project $name does not define $LagomApplicationLoaderPath path in configuration.")
        false
    }.orElse[IProject, Boolean] {
      case _ =>
        true
    }
  }
}