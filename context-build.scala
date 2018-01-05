import java.io._
import sys.process._

object `context-build` {

  private val BUILD_TYPE_RELEASE = "release"
  private val BUILD_TYPE_DEVELOPMENT = "development"

  def main(args: Array[String]) {
    val workspaceOfProjectCore = System.getenv().getOrDefault("TALNTS_WORKSPACE_CORE", "../talnts-core")
    val workspaceOfProjectAPI = System.getenv().getOrDefault("TALNTS_WORKSPACE_API", "../talnts-api")
    val workspaceOfProjectUI = System.getenv().getOrDefault("TALNTS_WORKSPACE_UI", "../talnts-ui")

    val projectBuildType = Option(System.getProperty("type")).getOrElse(BUILD_TYPE_DEVELOPMENT)

    val dependenciesBuildType = if (projectBuildType == "release") "-Dtype=" + BUILD_TYPE_RELEASE else "-Dtype=" + BUILD_TYPE_DEVELOPMENT

    Process(Seq("sbt", "-v", "clean", dependenciesBuildType, "publish-local"), new File(workspaceOfProjectCore)) !;
    Process(Seq("sbt", "-v", "clean", dependenciesBuildType, "publish-local"), new File(workspaceOfProjectAPI)) !;
    Process(Seq("sbt", "-v", "clean", dependenciesBuildType, "publish-local"), new File(workspaceOfProjectUI)) !;
  }
}