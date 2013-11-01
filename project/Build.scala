import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {
  val appName         = "pt"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq("com.datastax.cassandra" % "cassandra-driver-core" % "2.0.0-beta1")

  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here
    coffeescriptOptions := Seq("bare")
  )
}
