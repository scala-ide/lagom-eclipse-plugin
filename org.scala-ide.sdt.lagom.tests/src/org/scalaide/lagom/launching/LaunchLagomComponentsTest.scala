package org.scalaide.lagom.launching

import org.eclipse.debug.core.ILaunchManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scalaide.core.testsetup.IProjectHelpers
import org.scalaide.core.testsetup.TestProjectSetup

import LaunchLagomComponentsTest.project
import org.junit.BeforeClass

object LaunchLagomComponentsTest extends TestProjectSetup("lagom-test", bundleName = "org.scala-ide.sdt.lagom.tests")
    with LaunchUtils with IProjectHelpers {
  @BeforeClass def setup(): Unit = {
    cleanBuild(project)
  }
}

class LaunchLagomComponentsTest {
  import LaunchLagomComponentsTest._

  private val fileWithLaunchEffectName = "launch-lagom-component.result"

  @After def clean(): Unit = {
    file(fileWithLaunchEffectName).delete( /*force = */ true, /*monitor = */ null)
  }

  @Test def shouldLaunchCassandra(): Unit = {
    whenApplicationWasLaunchedFor("launch-cassandra")(project, ILaunchManager.RUN_MODE) {
      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
    }
  }

//  @Test def shouldLaunchKafka(): Unit = {
//    whenApplicationWasLaunchedFor("launch-kafka")(project, ILaunchManager.RUN_MODE) {
//      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
//    }
//  }
//
//  @Test def shouldLaunchLocator(): Unit = {
//    whenApplicationWasLaunchedFor("launch-locator")(project, ILaunchManager.RUN_MODE) {
//      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
//    }
//  }
//
//  @Test def shouldLaunchService(): Unit = {
//    whenApplicationWasLaunchedFor("launch-service")(project, ILaunchManager.RUN_MODE) {
//      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
//    }
//  }
}
