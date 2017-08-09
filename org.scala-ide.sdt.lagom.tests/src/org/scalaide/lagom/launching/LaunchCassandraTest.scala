package org.scalaide.lagom.launching

import org.eclipse.debug.core.ILaunchManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scalaide.core.testsetup.IProjectHelpers
import org.scalaide.core.testsetup.TestProjectSetup

import LaunchCassandraTest.file
import LaunchCassandraTest.project

object LaunchCassandraTest extends TestProjectSetup("lagom-test", bundleName = "org.scala-ide.sdt.lagom.tests")

class LaunchCassandraTest extends LaunchUtils with IProjectHelpers {
  override val launchConfigurationName = "launch-cassandra"
  private val fileWithLaunchEffectName = "launch-cassandra.result"
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