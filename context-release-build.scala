import java.io.File

import scala.sys.process._

object `context-release-build` {

  def main(args: Array[String]): Unit = {
    Process(Seq("scala", "context-init.scala", "-Dtype=release"), new File(".")) !
  }

}