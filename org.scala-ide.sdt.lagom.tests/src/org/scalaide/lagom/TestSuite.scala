package org.scalaide.lagom

import org.junit.runner.RunWith
import org.junit.runners.Suite

import org.scalaide.lagom.launching.LaunchCassandraTest

@RunWith(classOf[Suite])
@Suite.SuiteClasses(
  Array(classOf[LaunchCassandraTest]))
class TestSuite 
