package org.scalaide.lagom

import org.junit.runner.RunWith
import org.junit.runners.Suite

import org.scalaide.lagom.launching.LaunchLagomComponentsTest

@RunWith(classOf[Suite])
@Suite.SuiteClasses(
  Array(classOf[LaunchLagomComponentsTest]))
class TestSuite 
