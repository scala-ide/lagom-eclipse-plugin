package org.scalaide.lagom.launching

import org.eclipse.debug.core.ILaunchManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scalaide.core.testsetup.IProjectHelpers
import org.scalaide.core.testsetup.TestProjectSetup
import org.scalaide.debug.internal.launching.LaunchUtils

import LaunchKafkaTest.file
import LaunchKafkaTest.project

object LaunchKafkaTest extends TestProjectSetup("lagom-test", bundleName = "org.scala-ide.sdt.lagom.tests")

class LaunchKafkaTest extends LaunchUtils with IProjectHelpers {
  override val launchConfigurationName = "launch-kafka"
  private val fileWithLaunchEffectName = "launch-kafka.result"

  @Before def setup(): Unit = {
    cleanBuild(project)
  }

  @After def clean(): Unit = {
    file(fileWithLaunchEffectName).delete( /*force = */ true, /*monitor = */ null)
  }

  @Test def shouldLaunchTestInDebugMode(): Unit = {
    whenApplicationWasLaunchedFor(project, ILaunchManager.RUN_MODE) {
      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
    }
  }
}