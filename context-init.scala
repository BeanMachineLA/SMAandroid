import java.io.File

import scala.io._
import scala.sys.process._
import scala.util._

object `context-init` {

  private val BUILD_TYPE_RELEASE = "release"
  private val BUILD_TYPE_DEVELOPMENT = "development"

  val buildProperties = loadProperties("gradle.properties")

  val locationOfProjectCore = buildProperties("locationOfProjectCore")
  val locationOfProjectAPI = buildProperties("locationOfProjectAPI")
  val locationOfProjectUI = buildProperties("locationOfProjectUIKit")

  def main(args: Array[String]): Unit = {
    val projectBuildType = Option(System.getProperty("type")).getOrElse(BUILD_TYPE_DEVELOPMENT)

    val locationOfDependencies = buildProperties("locationOfDependencies")

    executeHere("rm", "-rf", locationOfDependencies)

    executeHere("mkdir", "-p", locationOfProjectCore)
    executeHere("mkdir", "-p", locationOfProjectAPI)
    executeHere("mkdir", "-p", locationOfProjectUI)

    executeInCore("git", "init")
    executeInAPI("git", "init")
    executeInUI("git", "init")

    executeInCore("git", "remote", "add", "origin", buildProperties("repositoryOfProjectCore"))
    executeInAPI("git", "remote", "add", "origin", buildProperties("repositoryOfProjectAPI"))
    executeInUI("git", "remote", "add", "origin", buildProperties("repositoryOfProjectUIKit"))

    executeInCore("git", "fetch", "origin")
    executeInAPI("git", "fetch", "origin")
    executeInUI("git", "fetch", "origin")

    val branchOfProjectCore = if (projectBuildType == BUILD_TYPE_DEVELOPMENT) buildProperties("branchOfProjectCore") else "master"
    val branchOfProjectAPI = if (projectBuildType == BUILD_TYPE_DEVELOPMENT) buildProperties("branchOfProjectAPI") else "master"
    val branchOfProjectUI = if (projectBuildType == BUILD_TYPE_DEVELOPMENT) buildProperties("branchOfProjectUIKit") else "master"

    executeInCore("git", "checkout", "-b", branchOfProjectCore, "origin/" + branchOfProjectCore)
    executeInAPI("git", "checkout", "-b", branchOfProjectAPI, "origin/" + branchOfProjectAPI)
    executeInUI("git", "checkout", "-b", branchOfProjectUI, "origin/" + branchOfProjectUI)

    val dependenciesBuildType = if (projectBuildType == BUILD_TYPE_DEVELOPMENT) "-Dtype=" + BUILD_TYPE_DEVELOPMENT else "-Dtype=" + BUILD_TYPE_RELEASE

    val dependenciesBuildMode = "-Dmode=orphan"

    executeInCore("sbt", "-v", "clean", "publish-local", dependenciesBuildType)
    executeInAPI("sbt", "-v", "clean", "publish-local", dependenciesBuildType, dependenciesBuildMode)
    executeInUI("sbt", "-v", "clean", "publish-local", dependenciesBuildType, dependenciesBuildMode)

    executeHere("rm", "-rf", locationOfDependencies)
  }

  def loadProperties(file: String): Map[String, String] = {
    val url = getClass.getResourceAsStream(file)

    Try(Source.fromInputStream(url)) match {
      case Success(source) => loadProperties(source)
      case Failure(problem) => Map()
    }
  }

  def loadProperties(source: Source): Map[String, String] = {
    Try(source.getLines()) match {
      case Success(lines) => lines.foldLeft(Map[String, String]())((properties, line) => {
        if (line.trim.isEmpty || line.startsWith("#")) {
          properties
        } else {
          val separatorIndex = line.indexOf('=')
          val key = line.substring(0, separatorIndex)
          val value = line.substring(separatorIndex + 1)

          properties + (key -> value)
        }
      })
      case Failure(problem) => Map()
    }
  }

  private def executeHere(command: String*) = {
    println("[info] Execute in current directory: " + command.toList)

    Process(command.toSeq, new File(".")) !
  }

  private def executeInCore(command: String*) = {
    println("[info] Execute in { Core } project directory: " + command.toList)

    Process(command.toSeq, new File(locationOfProjectCore)) !
  }

  private def executeInAPI(command: String*) = {
    println("[info] Execute in { API } project directory: " + command.toList)

    Process(command.toSeq, new File(locationOfProjectAPI)) !
  }

  private def executeInUI(command: String*) = {
    println("[info] Execute in { UI } project directory: " + command.toList)

    Process(command.toSeq, new File(locationOfProjectUI)) !
  }

}