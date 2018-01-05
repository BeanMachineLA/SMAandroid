import java.io._

import scala.io._
import scala.language.postfixOps
import scala.sys.process._
import scala.util._

object `context-release-publish` {

  val buildProperties = loadProperties("gradle.properties")

  val releaseVersionCode = buildProperties("projectVersionCode").toInt
  val releaseVersionName = buildProperties("projectVersionName")
  val releaseBranch = "release-" + releaseVersionName

  val snapshotVersionCode = increaseVersion(releaseVersionCode.toString).toInt
  val snapshotVersionName = increaseVersion(releaseVersionName)

  val coreVersion = increaseVersion(buildProperties("versionOfProjectCore"))

  val apiVersion = increaseVersion(buildProperties("versionOfProjectAPI"))

  val uiVersion = increaseVersion(buildProperties("versionOfProjectUI"))

  val releaseLocation = new File("release")

  def main(args: Array[String]): Unit = {
    executeHere("rm", "-rf", releaseLocation.getName)

    executeHere("mkdir", "-v", releaseLocation.getName)

    execute("mkdir", "-v", "dependencies")

    execute("git", "clone", buildProperties("repositoryOfProjectCore"), "dependencies" + File.separator + "talnts-core")
    execute("git", "clone", buildProperties("repositoryOfProjectAPI"), "dependencies" + File.separator + "talnts-api")
    execute("git", "clone", buildProperties("repositoryOfProjectUIKit"), "dependencies" + File.separator + "talnts-ui")

    executeInCore("git", "checkout", "-b", "develop", "origin/develop")
    executeInAPI("git", "checkout", "-b", "develop", "origin/develop")
    executeInUI("git", "checkout", "-b", "develop", "origin/develop")

    executeInCore("scala", "context-release-publish.scala")
    executeInAPI("scala", "context-release-publish.scala")
    executeInUI("scala", "context-release-publish.scala")

    executeInCore("git", "checkout", "master")
    executeInAPI("git", "checkout", "master")
    executeInUI("git", "checkout", "master")

    executeInCore("git", "pull")
    executeInAPI("git", "pull")
    executeInUI("git", "pull")

    executeInCore("scala", "context-release-install.scala")
    executeInAPI("scala", "context-release-install-orphan.scala")
    executeInUI("scala", "context-release-install-orphan.scala")

    execute("rm", "-rf", "dependencies")

    execute("git", "init")

    execute("git", "remote", "add", "origin", buildProperties("projectRepository"))

    execute("git", "fetch", "origin", "--depth=1")

    execute("git", "checkout", "-b", "develop", "origin/develop")

    execute("git", "checkout", "-b", releaseBranch, "develop")

    writeProperties(createProperties(
      releaseVersionCode,
      releaseVersionName,
      "master",
      "master",
      "master",
      coreVersion,
      apiVersion,
      uiVersion
    ))

    writeREADME(createREADME(
      releaseVersionName,
      "master"
    ))

    execute("git", "add", "--all")

    execute("git", "commit", "-m", "[RELEASE] " + releaseVersionName)

    execute("git", "checkout", "develop")

    execute("git", "merge", "--no-ff", "--no-commit", releaseBranch)

    writeProperties(createProperties(
      snapshotVersionCode,
      snapshotVersionName,
      "develop",
      "develop",
      "develop",
      coreVersion + "-SNAPSHOT",
      apiVersion + "-SNAPSHOT",
      uiVersion + "-SNAPSHOT"
    ))

    writeREADME(createREADME(
      snapshotVersionName,
      "develop"
    ))

    execute("git", "add", "--all", ":/")

    execute("git", "commit", "-m", s"Merge '$releaseBranch' to develop")

    execute("git", "checkout", releaseBranch)

    execute("git", "checkout", "master")

    execute("git", "merge", "--no-ff", "--no-commit", releaseBranch)

    writeProperties(createProperties(
      releaseVersionCode,
      releaseVersionName,
      "master",
      "master",
      "master",
      coreVersion,
      apiVersion,
      uiVersion
    ))

    writeREADME(createREADME(
      releaseVersionName,
      "master"
    ))

    execute("git", "checkout", "--theirs", ".")

    execute("git", "add", "--all", ":/")

    execute("git", "commit", "-m", s"Merge branch '$releaseBranch'")

    execute("git", "tag", "-a", releaseVersionName, "-m", releaseVersionName)

    execute("git", "push", "origin", "develop")

    execute("git", "push", "origin", "master")

    execute("git", "push", "origin", releaseVersionName)

    execute("gradle", "clean", "build")

    execute("curl", "https://app.testfairy.com/api/upload",
      "-F", "api_key=b676ebb0f76cf81152ea6b969abf40c1634edc4a",
      "-F", "file=@./app/build/outputs/apk/app-release.apk",
      "-F", "metrics=cpu,network,logcat",
      "-F", "testers_groups=talnts",
      "-F", "comment=RELEASE")
  }

  private def loadProperties(file: String): Map[String, String] = {
    val url = getClass.getResourceAsStream(file)

    Try(Source.fromInputStream(url)) match {
      case Success(source) => loadProperties(source)
      case Failure(problem) => Map()
    }
  }

  private def loadProperties(source: Source): Map[String, String] = {
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
    Process(command.toSeq, new File(".")) !
  }

  private def execute(command: String*) = {
    Process(command.toSeq, releaseLocation) !
  }

  private def executeInProject(projectLocation: String, command: Seq[String]) = {
    Process(command, new File(List(releaseLocation.getName, "dependencies", projectLocation).mkString(File.separator))) !
  }

  private def executeInCore(command: String*) = {
    executeInProject("talnts-core", command.toSeq)
  }

  private def executeInAPI(command: String*) = {
    executeInProject("talnts-api", command.toSeq)
  }

  private def executeInUI(command: String*) = {
    executeInProject("talnts-ui", command.toSeq)
  }

  private def executeAndOutput(command: String*) = {
    Process(command.toSeq, releaseLocation) !!
  }

  private def createProperties(versionCode: Int, versionName: String, coreBranch: String, apiBranch: String, uiBranch: String, coreVersion: String, apiVersion: String, uiVersion: String): String = {
    List (
      "locationOfDependencies=" + buildProperties("locationOfDependencies"),
      "locationOfProjectCore=" + buildProperties("locationOfProjectCore"),
      "locationOfProjectAPI=" + buildProperties("locationOfProjectAPI"),
      "locationOfProjectUIKit=" + buildProperties("locationOfProjectUIKit"),
      "",
      "repositoryOfProjectCore=" + buildProperties("repositoryOfProjectCore"),
      "repositoryOfProjectAPI=" + buildProperties("repositoryOfProjectAPI"),
      "repositoryOfProjectUIKit=" + buildProperties("repositoryOfProjectUIKit"),
      "",
      "branchOfProjectCore=" + coreBranch,
      "branchOfProjectAPI=" + apiBranch,
      "branchOfProjectUIKit=" + uiBranch,
      "",
      "versionOfProjectCore=" + coreVersion,
      "versionOfProjectAPI=" + apiVersion,
      "versionOfProjectUI=" + uiVersion,
      "",
      "projectRepository=" + buildProperties("projectRepository"),
      "projectVersionCode=" + versionCode,
      "projectVersionName=" + versionName,
      "",
      "ci.badge.url=" + buildProperties("ci.badge.url"),
      "ci.repository.url=" + buildProperties("ci.repository.url"),
      "",
      "org.gradle.jvmargs=-XX:MaxPermSize=512m"
    ) mkString System.lineSeparator
  }

  private def createREADME(version: String, branch: String): String = {
    List (
      "# Talnts Android / App / " + version,
      "",
      "[![Build Status](" + buildProperties("ci.badge.url") + branch + ")](" + buildProperties("ci.repository.url") + ")"
    ) mkString System.lineSeparator
  }

  private def writeProperties(content: String): Unit = {
    new PrintWriter("release" + File.separator + "gradle.properties") { write(content); close }
  }

  private def writeREADME(content: String): Unit = {
    new PrintWriter("release" + File.separator + "README.md") { write(content); close }
  }

  private def increaseVersion(version: String): String = {
    if (version.endsWith("-SNAPSHOT")) {
      increaseVersion(version.substring(0, version.indexOf("-")))
    } else {
      version.substring(0, version.lastIndexOf(".") + 1) + (version.substring(version.lastIndexOf(".") + 1).toInt + 1)
    }
  }

}