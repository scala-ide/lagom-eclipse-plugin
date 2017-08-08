package test

import java.io.FileWriter

object LagomTest {
  def main(args: Array[String]): Unit = {
    (new LagomTest).foo()
  }
}

class LagomTest {
  def foo(): Unit = {
    val writer = new FileWriter("launch-kafka.result")
    writer.write("success")
    writer.close
  }
}