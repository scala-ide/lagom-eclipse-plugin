package org.scalaide.lagom.launching

import org.eclipse.debug.core.ILaunchManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.scalaide.core.testsetup.IProjectHelpers
import org.scalaide.core.testsetup.TestProjectSetup

import LaunchLagomComponentsTest.project
import org.junit.BeforeClass
import org.junit.Ignore

object LaunchLagomComponentsTest extends TestProjectSetup("lagom-test", bundleName = "org.scala-ide.sdt.lagom.tests")
    with LaunchUtils with IProjectHelpers {
  @BeforeClass def setup(): Unit = {
    cleanBuild(project)
  }
}

/** These integration tests are not so helpful as it could be imagine. Actually they are testing eclipse launch framework
 *  more than real functionalities enclosed in launchers. Anyway they are kept for smoke sanity check.
 * 
 */
class LaunchLagomComponentsTest {
  import LaunchLagomComponentsTest._

  private val fileWithLaunchEffectName = "launch-lagom-component.result"

  @After def clean(): Unit = {
    file(fileWithLaunchEffectName).delete( /*force = */ true, /*monitor = */ null)
  }

  @Ignore("Potentially flaky because there is call to Maven repo.")
  @Test def shouldLaunchCassandra(): Unit = {
    whenApplicationWasLaunchedFor("launch-cassandra")(project, ILaunchManager.RUN_MODE) {
      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
    }
  }

  @Ignore("Potentially flaky because there is call to Maven repo.")
  @Test def shouldLaunchKafka(): Unit = {
    whenApplicationWasLaunchedFor("launch-kafka")(project, ILaunchManager.RUN_MODE) {
      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
    }
  }

  @Ignore("Potentially flaky because there is call to Maven repo.")
  @Test def shouldLaunchLocator(): Unit = {
    whenApplicationWasLaunchedFor("launch-locator")(project, ILaunchManager.RUN_MODE) {
      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
    }
  }

  @Ignore("""Cannot be run in current form because output forder is not added to runner classpath.
      It is required by reloadable server.
      Potentially flaky because there is call to Maven repo.""")
  @Test def shouldLaunchService(): Unit = {
    whenApplicationWasLaunchedFor("launch-service")(project, ILaunchManager.RUN_MODE) {
      assertLaunchEffect(project, ILaunchManager.RUN_MODE, file(fileWithLaunchEffectName))
    }
  }
}
