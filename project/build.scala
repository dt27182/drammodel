import sbt._
import Keys._

object BuildSettings extends Build {
  lazy val chisel = Project("chisel", file("chisel"))
  lazy val cpu = Project("cpu", file("src")).dependsOn(chisel)

}
