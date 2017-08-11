package org.scalaide.lagom

import org.junit.runner.RunWith
import org.junit.runners.Suite

import org.scalaide.lagom.launching.LaunchLagomComponentsTest
import org.scalaide.lagom.tools.ToolsTest

@RunWith(classOf[Suite])
@Suite.SuiteClasses(
  Array(classOf[LaunchLagomComponentsTest],
      classOf[ToolsTest]
  ))
class TestSuite 
