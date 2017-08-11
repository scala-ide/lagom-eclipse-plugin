package org.scalaide.lagom.tools

import org.junit.Test
import org.mockito.Mockito._
import org.eclipse.core.resources.IProject
import org.junit.Assert

class ToolsTest {
  import org.scalaide.lagom.eclipseTools
  @Test def shouldFindLagomLibWithScalaVersionAndLagomVersion(): Unit = {
    val tested = "lagom-lib_2.13-1.23.23.jar"

    eclipseTools.findScalaLagomVersion(tested).map {
      case v @ (sv, lv) =>
        Assert.assertTrue(("2.13", "1.23.23") == v)
      case _ => Assert.fail
    }
  }

  @Test def shouldFindLagomLibWithScalaVersionButNoLagomVersion(): Unit = {
    val tested = "lagom-lib_2.13.jar"

    eclipseTools.findScalaLagomVersion(tested).map {
      case v @ (sv, lv) =>
        Assert.assertTrue(("2.13", eclipseTools.DefaultVersions._2) == v)
      case _ => Assert.fail
    }
  }

  @Test def shouldFindLagomLibWithNoScalaVersionAndLagomVersion(): Unit = {
    val tested = "lagom-lib-1.23.23.jar"

    eclipseTools.findScalaLagomVersion(tested).map {
      case v @ (sv, lv) =>
        Assert.assertTrue((eclipseTools.DefaultVersions._1, "1.23.23") == v)
      case _ => Assert.fail
    }
  }

  @Test def shouldFindLagomLibWithBothNoScalaVersionAndLagomVersion(): Unit = {
    val tested = "lagom-lib.jar"

    eclipseTools.findScalaLagomVersion(tested).map {
      case v @ (sv, lv) =>
        Assert.assertTrue(eclipseTools.DefaultVersions == v)
      case _ => Assert.fail
    }
  }

  @Test def shouldNotFindLagomLib(): Unit = {
    val tested = "lib_2.13-1.23.23.jar"

    eclipseTools.findScalaLagomVersion.orElse[String, Any] {
      case _ => // ok
    }(tested)
  }
}
