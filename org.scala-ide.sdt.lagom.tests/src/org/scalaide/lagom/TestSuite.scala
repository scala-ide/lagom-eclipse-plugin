package org.scalaide.lagom

import org.junit.runner.RunWith
import org.junit.runners.Suite

import org.scalaide.lagom.launching.LaunchKafkaTest

@RunWith(classOf[Suite])
@Suite.SuiteClasses(
  Array(classOf[LaunchKafkaTest]))
class TestSuite 
